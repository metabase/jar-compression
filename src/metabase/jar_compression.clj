(ns metabase.jar-compression
  (:require [metabase.jar-compression
             [algorithms :as algorithms]
             [common :as common]
             [compress :as compress]
             [decompress :as decompress]]))

(defn compress!
  {:style/indent 1}
  [in & {:keys [out blacklist pack200 compression strip-directories strip-source]
         :or   {pack200           true
                strip-directories true
                strip-source      true
                compression       :xz}}]
  (let [out (or out (compress/default-compressed-filename in {:pack200 pack200, :compression compression}))]
    (assert (not= in out) "Input filename cannot be the same as output filename.")
    (println
     (format "Compressing %s -> %s " in out)
     "\n"
     (format "(pack200: %s, compression: %s, blacklist: %s, strip dirs: %s, strip source: %s)"
             (boolean pack200) compression (boolean blacklist) (boolean strip-directories) (boolean strip-source)))
    (with-open [is (compress/filtered-jar-input-stream
                    (common/->input-stream in)
                    (compress/make-blacklist-fn blacklist strip-directories strip-source))
                os (algorithms/compressed-output-stream compression (common/->output-stream out))]
      (if pack200
        (compress/write! :pack200 is os pack200)
        (compress/write! :default is os nil)))))


(defn decompress!
  {:style/indent 1}
  [in & {:keys [out pack200 compression], :or {pack200 ::guess}}]
  (let [pack200     (case pack200
                      ::guess (decompress/guess-pack200 in)
                      pack200)
        compression (or compression (decompress/guess-compression in))
        out         (or out (decompress/default-decompressed-filename in))]
    (assert (not= in out) "Input filename cannot be the same as output filename.")
    (println (format "Decompressing %s -> %s (pack200: %s, compression: %s)"
                     in out (boolean pack200) compression))
    (with-open [is (algorithms/compressed-input-stream compression (common/->input-stream in))
                os (common/->output-stream out)]
      (decompress/write! (if pack200 :pack200 :default) is os))))
