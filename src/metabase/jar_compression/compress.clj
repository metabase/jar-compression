(ns metabase.jar-compression.compress
  (:require [clojure.string :as str]
            [metabase.jar-compression.common :as common])
  (:import [java.io InputStream OutputStream]
           [java.util.jar JarInputStream JarOutputStream Pack200 Pack200$Packer]
           java.util.zip.ZipEntry))

(defn- maybe-slurp [filename-or-coll]
  (set (if (string? filename-or-coll)
         (some-> filename-or-coll slurp str/split-lines)
         filename-or-coll)))

;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                    Filtered JAR Input Stream (Blacklisting)                                    |
;;; +----------------------------------------------------------------------------------------------------------------+

(defn filtered-jar-input-stream
  ^JarInputStream [^InputStream is, blacklist?]
  (proxy [JarInputStream] [is false]
    (getNextJarEntry []
      (let [this  ^JarInputStream this
            entry (proxy-super getNextJarEntry)]
        (if-not (blacklist? entry)
          entry
          (recur this))))
    (getNextEntry []
      (let [this  ^JarInputStream this
            entry (proxy-super getNextEntry)]
        (if-not (blacklist? entry)
          entry
          (recur this))))))

(defn make-blacklist-fn
  "Make a function that can be used to filter out ZipEntries from the source JAR. `blacklist` is an optional list of
  classnames to exclude, or the name of a file containing classnames separated by newlines."
  [blacklist strip-directories?]
  (let [filter-blacklist
        (when blacklist
          (let [blacklist (maybe-slurp blacklist)]
            (fn [^ZipEntry entry]
              (when (contains? blacklist (str entry))
                (println "Skipping file in blacklist:" (str entry))
                true))))

        filter-directories
        (when strip-directories?
          (fn [^ZipEntry entry]
            (when (.isDirectory entry)
              (println "Skipping directory:" (str entry))
              true)))

        blacklist-fns (filter some? [filter-blacklist filter-directories])]
    (if (seq blacklist-fns)
      (apply some-fn blacklist-fns)
      (constantly false))))


;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                     write!                                                     |
;;; +----------------------------------------------------------------------------------------------------------------+

(defmulti write!
  {:arglists '([method is os options])}
  (fn [method & _]
    method))

;;; ----------------------------------------------- write! -- pack200 ------------------------------------------------

(defn- pack200-packer-with-options
  ^Pack200$Packer [{:keys [classes-to-skip]}]
  (let [packer     (Pack200/newPacker)
        properties (.properties packer)]
    (.put properties Pack200$Packer/EFFORT "9")
    (.put properties Pack200$Packer/UNKNOWN_ATTRIBUTE Pack200$Packer/PASS)
    (loop [i 0, [^String class-to-skip & more] (maybe-slurp classes-to-skip)]
      (when class-to-skip
        (.put properties (str Pack200$Packer/PASS_FILE_PFX i) class-to-skip)
        (recur (inc i) more)))
    packer))

(defmethod write! :pack200
  [_, ^JarInputStream is, ^OutputStream os, opts]
  (let [packer (pack200-packer-with-options opts)
        pack!  (future (.pack packer is os))]
    (while (not (realized? pack!))
        (println (format "Packing: progress %s %%" (.get (.properties packer) Pack200$Packer/PROGRESS)))
        (Thread/sleep 2000))
      (println "Packing: finished.")))


;;; ----------------------------------------------- write! -- default ------------------------------------------------

(defmethod write! :default
  [_, ^JarInputStream is, ^OutputStream os, _]
  (with-open [os (JarOutputStream. os)]
    (.setLevel os 9)
    (common/copy-entries! is os)))


;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                 Other Helpers                                                  |
;;; +----------------------------------------------------------------------------------------------------------------+

(defn default-compressed-filename
  ^String [source-filename {:keys [pack200 compression]}]
  (cond-> (common/->filename source-filename
            "Can't pick default compressed filename when source isn't a filename; please specify output filename or OutputStream by setting :out.")
    pack200     (str ".pack")
    compression (str (when (not= (or compression
                                     (throw (Exception. "Error: invalid value for compression")))
                                 :none)
                       (str \. (name compression))))))
