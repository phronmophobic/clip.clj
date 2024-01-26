(ns com.phronemophobic.clip
  (:require [com.phronemophobic.clip.raw :as raw]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as str])
  (:import com.sun.jna.ptr.FloatByReference
           com.sun.jna.Memory
           com.sun.jna.Structure
           com.sun.jna.Pointer
           java.lang.ref.Cleaner)
  (:gen-class))

(raw/import-structs!)

(defn ^:private make-clip-image-u8 []
  (let [img* (raw/make_clip_image_u8)
        ptr (.getPointer ^Structure img*)]
    (.register ^Cleaner raw/cleaner img*
               (fn []
                 (raw/delete_clip_image_u8 ptr)))
    img*))

(defn ^:private make-clip-image-f32 []
  (let [img* (raw/make_clip_image_f32)
        ptr (.getPointer ^Structure img*)]
    (.register ^Cleaner raw/cleaner img*
               (fn []
                 (raw/delete_clip_image_f32 ptr)))
    img*))

(defn image-embedding [ctx f]
  (let [f (io/as-file f)
        path (.getCanonicalPath f)

        img* (make-clip-image-u8)
        img-res* (make-clip-image-f32)

        params (raw/clip_get_vision_hparams ctx)
        vec-dim (:projection_dim params)

        _ (when (zero? (raw/clip_image_load_from_file path img*))
            (throw (ex-info "Could not load image."
                            {:ctx ctx
                             :f f}))
            )

        _ (when (zero? (raw/clip_image_preprocess ctx img* img-res*))
            (throw (ex-info "Could not preprocess image."
                            {:ctx ctx
                             :f f})))

        img-vec (Memory.
                 (*
                  ;; 4 bytes per float
                  4
                  ;; vec-dim floats
                  vec-dim))
        _ (when (zero?
                 (raw/clip_image_encode
                  ctx 8 img-res* img-vec 1))
            (throw (ex-info "Could not encode image."
                            {:ctx ctx
                             :f f})))]
    (.getFloatArray img-vec 0 vec-dim)))


(defn text-embedding [ctx text]
  (let [tokens* (clip_tokensByReference.)
        _ (raw/clip_tokenize ctx text tokens*)

        params (raw/clip_get_vision_hparams ctx)
        vec-dim (:projection_dim params)

        vec (Memory.
             (*
              ;; 4 bytes per float
              4
              ;; vec-dim floats
              vec-dim))
        _ (when (zero?
                 (raw/clip_text_encode ctx 8 tokens* vec 1))
            (throw (ex-info "Could not encode text."
                            {:ctx ctx
                             :text text})))]
    (.getFloatArray vec 0 vec-dim)))

(defn cosine-similarity [^floats emb1 ^floats emb2]
  (let [num (alength emb1)]
    (loop [dot-product (float 0)
           n 0]
      (if (< n num)
        (recur (+
                dot-product
                (* (aget emb1 n)
                   (aget emb2 n)))
               (inc n))
        dot-product))))

(comment
  (do
    (def model-path
      "models/CLIP-ViT-B-32-laion2B-s34B-b79K_ggml-model-f16.gguf")

    (def ctx (raw/clip_model_load model-path 1)))
  (text-embedding ctx "hello")
  (image-embedding ctx "aimages/005bce0c-710f-4b3d-8c94-5be8d86585e9.jpg")

  ,)
