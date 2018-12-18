(ns metabase.jar-compression.algorithms
  "TODO - I'm pretty sure there's already an Apache commons library that does something just like this. Maybe we should
  use that instead of reinventing the wheel."
  (:import [java.io InputStream OutputStream]
           [java.util.zip GZIPInputStream GZIPOutputStream]
           [org.tukaani.xz LZMA2Options XZInputStream XZOutputStream]))

(defmulti ^InputStream compressed-input-stream
  {:arglists '([method is])}
  (fn [method & _]
    method))

(defmulti ^OutputStream compressed-output-stream
  {:arglists '([method os])}
  (fn [method & _]
    method))


(defmethod compressed-input-stream :none [_ os]
  os)

(defmethod compressed-output-stream :none [_ os]
  os)


(defmethod compressed-input-stream :gz [_, ^InputStream os]
  (GZIPInputStream. os))

(defmethod compressed-output-stream :gz [_, ^OutputStream os]
  (GZIPOutputStream. os))


(defmethod compressed-input-stream :xz [_, ^InputStream os]
  (XZInputStream. os))

(defmethod compressed-output-stream :xz [_, ^OutputStream os]
  (XZOutputStream. os (LZMA2Options. LZMA2Options/PRESET_MAX)))
