(ns wikiart.util
  (:require [com.phronemophobic.clip :as clip]
            [com.phronemophobic.usearch :as usearch]
            [datalevin.core :as d]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def clip-ctx
  (delay
    (clip/create-context "models/CLIP-ViT-B-32-laion2B-s34B-b79K_ggml-model-f16.gguf")))

(def data-dir (io/file "data" "wikiart" "data"))

(def image-table "image-table")
(def db (delay
          (let [db (d/open-kv "wikiart.db")]
            (d/open-dbi db image-table)
            db)))

(def dim 512)
(def index
  (delay
    (let [index (usearch/init {:dimensions dim
                               :metric :metric/ip
                               :quantization :quantization/f32})]
      (when (.exists (io/file "wikiart.usearch"))
        (usearch/load index "wikiart.usearch"))
      index)))



(def parquet-files
  (sort-by
   (fn [f]
     (.getName f))
   (into []
         (filter #(str/ends-with? (.getName %) ".parquet"))
         (file-seq data-dir))))

(defn index->key [[fileno idx]]
  (bit-or
   (bit-shift-left fileno 32)
   idx))

(defn key->index [key]
  [(unsigned-bit-shift-right key 32)
   (bit-and
    0xFFFFFFFF
    key)])


(def tmp-file (io/file "tmp"))         
(defn image->embedding [bs]
  (with-open [is (io/input-stream bs)]
    (io/copy is tmp-file))
  (clip/image-embedding @clip-ctx tmp-file))


(defn key->image [key]
  (d/get-value @db image-table key))
