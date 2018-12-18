(ns metabase.jar-compression.decompress
  (:require [clojure.string :as str]
            [metabase.jar-compression.common :as common])
  (:import [java.io InputStream OutputStream]
           [java.util.jar JarOutputStream Pack200]
           org.apache.commons.io.IOUtils))

;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                     write!                                                     |
;;; +----------------------------------------------------------------------------------------------------------------+

(defmulti write!
  {:arglists '([method is os])}
  (fn [method & _]
    method))

(defmethod write! :pack200
  [_, ^InputStream is, ^OutputStream os]
  (with-open [os (JarOutputStream. os)]
    (.unpack (Pack200/newUnpacker) is os)))

(defmethod write! :default
  [_, ^InputStream is, ^OutputStream os]
  (IOUtils/copy is os))


;;; +----------------------------------------------------------------------------------------------------------------+
;;; |                                                 Other Helpers                                                  |
;;; +----------------------------------------------------------------------------------------------------------------+

(defn default-decompressed-filename ^String [source-filename]
  (str/replace
   (common/->filename source-filename
     "Can't pick default decompressed filename when source isn't a filename; please specify output filename or OutputStream by setting :out.")
   #"(?:\.pack)?(?:\.[^\.]+)?$" ""))

(defn guess-compression [source-filename]
  (let [filename (common/->filename source-filename
                   "Can't guess compression if input isn't a filename; please set :compression.")]
    (or (when-let [[_ extension] (re-find #"(?:\.pack)?(?:\.(?!pack)([^\.]+)$)" filename)]
          (keyword extension))
        :none)))

(defn guess-pack200 [source-filename]
  (let [filename (common/->filename source-filename
                   "Can't guess whether pack200 was used if input isn't a filename; please set :pack200.")]
    (re-find #"(\.pack)(?:\.[^\.]+)?$" filename)))
