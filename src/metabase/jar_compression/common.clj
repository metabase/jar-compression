(ns metabase.jar-compression.common
  (:import [java.io InputStream OutputStream]
           [java.nio.file Files FileSystems OpenOption Path]
           [java.util.jar JarEntry JarInputStream JarOutputStream]
           org.apache.commons.io.IOUtils))

(defmacro ^:private varargs
  "Make a properly-tagged Java interop varargs argument."
  [klass & objects]
  (vary-meta `(into-array ~klass [~@objects])
             assoc :tag (format "[L%s;" (.getCanonicalName ^Class (ns-resolve *ns* klass)))))

(defn- get-path ^Path [^String path-component]
  (let [path (.getPath (FileSystems/getDefault) path-component (varargs String))]
    (if (.isAbsolute path)
      path
      (.getPath (FileSystems/getDefault) (System/getProperty "user.dir") (varargs String path-component)))))


;;; ---------------------------------------- ->input-stream & ->output-stream ----------------------------------------

(defmulti ^InputStream ->input-stream
  {:arglists '([x])}
  class)

(defmethod ->input-stream Path [^Path x]
  (Files/newInputStream x (varargs OpenOption)))

(defmethod ->input-stream String [x]
  (->input-stream (get-path x)))

(defmethod ->input-stream InputStream [x]
  x)

(defmulti ^OutputStream ->output-stream
  {:arglists '([x])}
  class)

(defmethod ->output-stream Path [^Path x]
  (Files/newOutputStream x (varargs OpenOption)))

(defmethod ->output-stream String [x]
  (->output-stream (get-path x)))

(defmethod ->output-stream OutputStream [x]
  x)

;;; --------------------------------------------------- ->filename ---------------------------------------------------

(defmulti ^String ->filename
  {:style/indent 1, :arglists '([x failure-message])}
  (fn [x & _]
    (class x)))

(defmethod ->filename String [x _]
  x)

(defmethod ->filename Path [x _]
  (str x))

(defmethod ->filename :default [_ failure-message]
  (throw (Exception. (str failure-message))))


;;; ----------------------------------------------- Copying JAR -> JAR -----------------------------------------------

(defn- copy-entry! [^JarInputStream is, ^JarOutputStream os, ^JarEntry entry]
  (.putNextEntry os (JarEntry. (.getName entry)))
  (IOUtils/copy is os)
  (.closeEntry os))

(defn copy-entries! [^JarInputStream is, ^JarOutputStream os]
  (loop []
    (when-let [entry (.getNextJarEntry is)]
      (copy-entry! is os entry)
      (recur))))
