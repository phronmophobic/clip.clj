(ns wikiart.ui
  (:require [wikiart.util :as util]
            [membrane.skia :as skia]
            [com.phronemophobic.usearch :as usearch]
            [com.phronemophobic.clip :as clip]
            [membrane.ui :as ui]
            [membrane.basic-components :as basic]
            [membrane.component :refer [defui defeffect]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.core.async :as async]))

(defui search-ui [{:keys [text results]}]
  (ui/vertical-layout
   (ui/horizontal-layout
    (basic/button {:text "X"
                   :on-click
                   (fn []
                     [[:set $text ""]])})
    (basic/textarea {:text text}))
   results))


(defonce app-state
  (atom {:text ""
         :result nil}))

(defn find-nearest [emb]
  (let [results (->> (usearch/search @util/index
                                      emb
                                      12)
                     (map first)
                     (map util/key->image))]
    results))

(defn bytes->image [bs]
  (skia/skia-load-image-from-memory bs))

(defn repaint! []
  (when-let [f (::repaint! @app-state)]
    (f)))

(def results-chan
  (let [ch (async/chan (async/sliding-buffer 1))]
    (async/thread
      (try
        (loop []
          (when-let [s (async/<!! ch)]
            (let [emb (if (str/starts-with? s "http")
                        (try
                          (util/image->embedding (io/as-url s))
                          (catch Exception e
                            (clip/text-embedding @util/clip-ctx "not found")))
                        (clip/text-embedding @util/clip-ctx s))

                  nearest (usearch/search @util/index
                                          emb
                                          12)
                  
                  imgs (->> nearest
                            (map (fn [[key distance]]
                                   (let [img-bytes (util/key->image key)
                                         img (bytes->image img-bytes)]
                                     [(ui/image img [250 nil])
                                      (ui/fill-bordered
                                       [1 1 1]
                                       3
                                       (ui/label (format "%.3f" distance)))]))))
                  table (ui/vertical-layout
                         (ui/label s)
                         (ui/table-layout
                          (into []
                                (partition-all 4)
                                imgs)))]
              (swap! app-state
                     assoc :results table)
              (repaint!))
            (recur)))
        (catch Exception e
          (prn e))))
    ch))

(add-watch app-state ::update-results
           (fn [_ _ old new]
             (when (not= (:text old)
                         (:text new))
               (async/put! results-chan (:text new)))))


(defn show! []
  (let [winfo (skia/run
                (membrane.component/make-app #'search-ui
                                             app-state))]
    (swap! app-state assoc ::repaint! (::skia/repaint winfo))
    nil))




