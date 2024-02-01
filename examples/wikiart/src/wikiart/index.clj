(ns wikiart.index
  (:require [com.phronemophobic.clip :as clip]
            [com.phronemophobic.usearch :as usearch]
            [wikiart.util :as util]
            [datalevin.core :as d]
            [nextjournal.clerk :as clerk]
            [tech.v3.libs.parquet :as pq]
            [taoensso.nippy :as nippy]
            [tech.v3.dataset :as ds]
            [clojure.string :as str]
            [clojure.java.io :as io])
  (:import javax.imageio.ImageIO
           java.io.ByteArrayInputStream))


(defn index-images []
  (let [db @util/db
        kv-transaction
        (eduction
         (map-indexed vector)
         (mapcat (fn [[fileno f]]
                   (eduction
                    (map-indexed (fn [idx bs]
                                   [(util/index->key [fileno idx]) bs]))
                    (get (pq/parquet->ds f)
                         "image.bytes"))))
         (map (fn [[k v]]
                [:put util/image-table k v]))
         util/parquet-files)]
    (time
     (d/transact-kv db
                    kv-transaction))))


(defn index-dataset [index fileno ds]
  (let [images (get ds "image.bytes")]
    (usearch/reserve index
                     (+ (count index)
                        (count images)))
    (doseq [[i bs] (map-indexed vector images)]
      (when-let [emb (try
                       (util/image->embedding bs)
                       (catch Exception e
                         (prn e)
                         nil))]
        (usearch/add index (util/index->key [fileno i]) emb)))))

(defn index-all-embeddings [index files]
  (let [file-count (count files)]
    (doseq [[fileno f] (map-indexed vector files)]
      (prn "indexing " (str fileno "/" file-count) (.getName f))
      (let [ds (pq/parquet->ds f)]
        (index-dataset index fileno ds)))))

(defn index-embeddings []
  (time
   (binding [clip/*num-threads* 8]
     (index-all-embeddings @util/index util/parquet-files)))

  (usearch/save @util/index
                "wikiart.usearch"))





(defn index-all [& args]
  (index-images)
  (index-embeddings))
