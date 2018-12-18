(ns metabase.jar-compression
  (:require [metabase.jar-compression
             [algorithms :as algorithms]
             [common :as common]
             [compress :as compress]
             [decompress :as decompress]]))

(defn compress!
  {:style/indent 1}
  [in & {:keys [out blacklist pack200 compression strip-directories]
         :or   {pack200           true
                strip-directories true
                compression       :xz}
         :as   opts}]
  (let [out (or out (compress/default-compressed-filename in opts))]
    (println (format "Compressing %s -> %s (pack200: %s, compression: %s, blacklist: %s, strip dirs: %s)"
                     in out (boolean pack200) compression (boolean blacklist) (boolean strip-directories)))
    (with-open [is (compress/filtered-jar-input-stream
                    (common/->input-stream in)
                    (compress/make-blacklist-fn blacklist strip-directories))
                os (algorithms/compressed-output-stream compression (common/->output-stream out))]
      (if pack200
        (compress/write! :pack200 is os pack200)
        (compress/write! :default is os nil)))))


(defn decompress!
  {:style/indent 1}
  [in & {:keys [out pack200 compression], :or {pack200 ::guess}, :as opts}]
  (let [pack200     (case pack200
                      ::guess (decompress/guess-pack200 in)
                      pack200)
        compression (or compression (decompress/guess-compression in))
        out         (or out (decompress/default-decompressed-filename in))]
    (println (format "Decompressing %s -> %s (pack200: %s, compression: %s)"
                     in out (boolean pack200) compression))
    (with-open [is (algorithms/compressed-input-stream compression (common/->input-stream in))
                os (common/->output-stream out)]
      (decompress/write! (if pack200 :pack200 :default) is os))))
