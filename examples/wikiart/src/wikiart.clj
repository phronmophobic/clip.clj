^{:nextjournal.clerk/visibility {:code :hide :result :hide}
  :nextjournal.clerk/toc true}
(ns wikiart
  (:require [com.phronemophobic.clip :as clip]
            [com.phronemophobic.usearch :as usearch]
            [wikiart.util :as util]
            [datalevin.core :as d]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as v]
            [membrane.ui :as ui]
            [membrane.java2d :as java2d]
            [clojure.string :as str]
            [clojure.java.io :as io])
  (:import javax.imageio.ImageIO
           java.io.ByteArrayInputStream))

{:nextjournal.clerk/visibility {:code :hide :result :hide}}

(comment
  (def log (atom []))

  (def var-viewer
    {:pred (fn [o]
             (swap! log conj o)
             true)
     :render-fn '(fn [& args]
                   nil)})

  (clerk/add-viewers! [var-viewer])
  (clerk/reset-viewers!
   (into [var-viewer]
         (remove #{v/var-viewer
                   v/var-from-def-viewer})
         v/default-viewers))
  ,)

(def float-class (Class/forName "[F"))
(defn float-array? [o]
  (instance? float-class o))
(def float-array-viewer
  {:pred float-array?
   :transform-fn
   (clerk/update-val vec)})
(clerk/add-viewers! [float-array-viewer])

(defn search [emb]
  (let [results (->> (usearch/search @util/index
                                     emb
                                     4)
                     (map first)
                     (map util/key->image)
                     (map (fn [bs]
                            (let [buf-img (with-open [bs (io/input-stream bs)]
                                            (ImageIO/read bs))
                                  resized
                                  (java2d/draw-to-image
                                   (ui/image buf-img
                                             [300 nil]))]
                              resized))))]
    (apply clerk/row results)))

(defn search-text [text]
  (search (clip/text-embedding @util/clip-ctx text)))

(defn search-image [url]
  (search (util/image->embedding (io/as-url url))))

{:nextjournal.clerk/visibility {:code :show :result :show}}

;; # Semantic Image Search with Clojure

;; In this post we'll be exploring the ideas behind semantic image search and vector databases using the [wikiart dataset](https://huggingface.co/datasets/huggan/wikiart). Our goal is to be able to search the 81,444 available images using plain english. On the implementation side, we'd like to be really lazy. Ideally, we'd like to shove all the images into a "vectordb", pass it the plain english query, and get reasonable results.

;; For the most part it works.

(search-text "farm animals, cute, cuddly")
(search-text "knight")
(search-text "medusa")
(search-text "green and blue, bouquet")
(search-text "da vinci sketch")
(search-text "twins")

;; ## Fuzzy Matching

;; Even if the dataset doesn't have an exact match, it often does a good job finding "similar" ideas.

(search-text "bigfoot")
(search-text "lord of the rings")
(search-text "hogwarts")

;; ## Reverse Image Search

;; We can also search for "similar" images to an input image.

^{:nextjournal.clerk/visibility {:code :hide :result :show}} 
(with-open [is (io/input-stream
                (io/file "data" "kitten-small.jpg"))]
  (ImageIO/read is))


;; source: https://en.m.wikipedia.org/wiki/File:Photo_of_a_kitten.jpg
(search-image (io/file "data" "kitten.jpg"))


;; ## Implementation

;; To implement our semantic search, we use an [OpenClip](https://github.com/mlfoundations/open_clip) [model](https://huggingface.co/laion/CLIP-ViT-B-32-laion2B-s34B-b79K) to create vector embeddings. We then use [usearch](https://github.com/unum-cloud/usearch/) to store the vectors and find matching results. If that all sounded like gibberish, don't worry. We'll cover the basics.

;; To explain how everything works, let's list the steps that were taken and then we'll explain the reasoning.

;; Prerocessing steps:
;; 1. Download all the data and models
;; 2. Derive an embedding vector for each image
;; 3. Put all the embedding vectors in a vector database (we used usearch)

;; Query Steps:
;; 1. Create an embedding vector for the query string
;; 2. Search the vector database for 4 image vectors that are nearest to the embedding vector for our query string

;; That's it!

;; ### Embedding Vectors

;; Based on the implementation, it seems like "embedding vectors" are important, but what are they? As clojurians, we know how to solve that problem. Let's check the etymology. The term "embedding" comes from mathematics:

;; > In mathematics, an embedding (or imbedding[1]) is one instance of some mathematical structure contained within another instance, such as a group that is a subgroup.

;; > When some object X is said to be embedded in another object Y, the embedding is given by some injective and structure-preserving map f : X → Y f:X → Y. The precise meaning of "structure-preserving" depends on the kind of mathematical structure of which X and Y are instances. In the terminology of category theory, a structure-preserving map is called a morphism.

;; _source: https://en.wikipedia.org/wiki/Embedding_

(search-text "uh oh")

;; Hmmmm, that didn't help. Let's try something else. Forget about category theory for a second and recall the game "20 questions". In the game 20 questions, one person picks a secret, random object. The object is usually something like a bike, chair, or maybe even an elephant. It doesn't matter. The point of the game is for the other player(s) to try to guess secret object. The guessers are allowed to ask up to 20 yes or no questions that the object picker must answer truthfully.

;; Some example questions a guesser might ask:
;;- "Is it alive?"
;;- "Can you eat it?"
;;- "Is it bigger than a microwave?"

;; The surprising thing about the game 20 questions is that the guessers actually have a pretty decent chance of figuring out the secret object. That's interesting, but what does that have to do with anything? It turns out the answers given to the guesser's questions are similar to an embedding vector.

;; While guessing in a game of 20 questions, you often pick different questions depending on all the answers you've received so far, but let's pretend that you ask the same 20 questions every time. We can represent all the responses as a vector of 20 boolean values:

(into []
      (map (fn [_] (rand-nth [true false]) ))
      (range 20))

;; The first element of the vector corresponds to the answer for the first question, second element to the answer of the second question, and so on.

;; If you can imagine filling out the answers for a bunch of objects, you can start to gain intuition for these vectors. One thing you might notice is that similar objects will have similar vectors (maybe only one or two answers differ). Another thing you might realize is that it makes a big difference _which_ questions you ask. As a dumb example, if you ask both "Is it bigger than house?" and "Is it bigger than a tree?", the answers will largely overlap which makes one of the questions redundant.

;; You can also imagine that if you ask enough questions, you might be able to uniquely identify just about any object.

;; Finally, you may notice that it can be difficult to give a hard yes or no to particular questions. Limiting responses to just yes or no is unnecessarily restrictive. For example, if someone asks "Is it edible?" and the secret object is a tarantula, you might want to be able to respond with some sort of "ehh".

(search-text "Is it edible?")

;; One way to allow more descriptive answers than yes/no is to allow the answer to represent where on the spectrum of yes/no the answers lies. So if the answer is halfway between yes and no, you might represent the answer as 0.5.

(into []
      (map (fn [_] (rand)))
      (range 20))

;; This vector of floating point numbers is essentially the embedding vector we were trying to figure out. **An embedding vector can be thought of as the collection of answers to a series of questions.** Typically, if a vector has answers to `n` questions, then we say that it has `n` dimensions. Figuring out things like which "questions" ask, how many is the right number, and even filling in the answers for a given image or query is beyond the scope of this post, but hopefully this analogy gives you an intuition for what we're dealing with here. For more information, you can check out the research behind [clip](https://openai.com/research/clip).

;; _Note: If you read about embedding vectors, no one will describe them as having answers to a series of questions. This is really about building a mental model for how these things behave._

;; Let's take a look at a real embedding vector.

(clip/text-embedding @util/clip-ctx "Is it edible?")

;; We can also create embedding vectors for images.

(util/image->embedding (io/file "data" "kitten.jpg"))

;; The main differences between these real vectors and our hypothetical vector is that the values of this vector are in the range of [-1, 1] and that there are 512 values (or dimensions).

;; ### Comparing Embedding Vectors

;; Hopefully, we now have a fuzzy idea of what an embedding vector is. How does that help with search? Going back to our 20 questions thought experiment, you may remember that similar objects will have similar answers, and hence, have similar vectors. Even though we've upgraded our vectors by making them longer and giving answers on a spectrum, the same idea still applies. Similar objects will correspond to similar vectors.

;; We didn't actually specify an implementation for calculating the similarity between vectors. For the simple case of yes/no answers, one obvious way to do it is to just count the number questions where two vectors give same answer. We can then use the number of shared answers as a similarity score. Another way to think about it is to count the number of questions where the answer differs. Counting mismatched answers gives us a _difference_ score rather than a _similarity_ score. It turns out that for more complicated cases, thinking about the _difference_ between vectors is easier than thinking about their similarity.

;; Unfortunately, our vectors are full of floating point numbers, not booleans. There's not just one obvious way to calculate the distance between two vectors. It seems like there would be many ways and you would be right. One way to calculate the distance between two vectors is to just do elementwise subtraction between the two vectors and add up all the differences. This distance metric is called the Manhattan distance. For some of you, this may be giving flashbacks to one of your classes from school years ago. Even though we're dealing with vectors with hundreds of elements, the distance metrics we use for 1d, 2d, and 3d space can apply to our n-dimensional embedding vectors. For example, we can use  euclidean distance for our distance metric. There are also about a dozen others. The `usearch` library has the following: cos, divergence, hamming, haversine, ip, jaccard, l2sq (euclidean), pearson, sorensen, and tanimoto. I don't even know what half of them do, but the point is we have options.

(search-text "euclidean geometry")

;; The only job our `usearch` vectordb does is store all the embedding vectors of our images and try to find the `n` "closest" vectors given a query vector. Calculating distances is generally pretty fast so using a vectordb isn't even necessary for many (most?) use cases.

;; Just to check our sanity, we can calculate distances between some vectors manually.

^{:nextjournal.clerk/visibility {:code :hide :result :show}} 
(with-open [is (io/input-stream
                (io/file "data" "kitten-small.jpg"))]
  (ImageIO/read is))

(def image-embedding
  (util/image->embedding (io/file "data" "kitten.jpg")))

(defn cosine-similarity
  "Returns the cosine similarity between two embeddings as a float in [0.0, 1.0].

  The embeddings should be float arrays."
  [^floats emb1 ^floats emb2]
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

(clerk/table
 (clerk/use-headers
  (into
   [["query text" "distance"]]
   (map (fn [txt]
          (let [text-embedding (clip/text-embedding @util/clip-ctx txt)]
            [txt
             (format "%.3f"
                     (- 1.0
                        (cosine-similarity
                         image-embedding
                         text-embedding)))])))
   ["cute fuzzy white kitten with a fabric background"
    "cute fuzzy white kitten"
    "cat"
    "cute"
    "fuzzy"
    "banana"
    "programming language"
    "clojure"])))

;; As you can see, the distance between "clojure" and the kitten image is much farther than the distance between the image and "cute fuzzy white kitten". The reason there's still distance between "cute fuzzy white kitten with a fabric background" and the kitten image is that you would have to perfectly describe the kitten image with your embedding to have a distance of zero. Our description is pretty close, but you could still include more details to distinguish this image from other images with cute kittens.

;; ## Limitations

;; Overall, I'm pretty happy with the results, but there are still some rough spots. For whatever reason, there are set of black and white images that tend score highly for unrelated queries.

(search-text "coffee")

;; You might convince yourself that Mr. Mustache actually has some coffee-like essence, but he and his friends show up everywhere.

(search-text "beer")
(search-text "samus")
(search-text "ending")

;; I haven't been showing the distance scores in the results, but these mismatched images do tend to have bad scores. However, the trouble is that it's not clear how to only show "good" results. It seems like you could limit results to images with scores better than some cutoff, but that doesn't seem to work.

;; I've also noticed the distance metric used has a big influence on the quality of the results. This seems pretty obvious in retrospect, but it's not totally clear what to do about it. There's no metric that seems to be categorically better and I haven't found any better advice than "try them".

;; It's likely that most of these problems are well known and probably even have fixes or workarounds. If you have any insights, [let me know](https://github.com/phronmophobic/clip.clj/issues)!

;; Also, you may be wondering if the image results shown in this post are cherry picked, and they totally are, but not by much! The results really are pretty good for the most part. I do think the model struggles a bit since these art pieces are probably different than the original training set, but that's kind of the point. We want a tool that has general applicability without a lot of fuss!


;; ## Conclusion

;; Exploring vector databases by implementing semantic image search was fun, interesting, and not too much work. It's pretty surprising that finding matching images just from a plain english query and access to the raw images mostly just works. There are still a few kinks to work out, but it seems like progress is forthcoming.

(search-text "nap time!")

^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
(comment
 ;; start Clerk's built-in webserver on the default port 7777, opening the browser when done
  (clerk/serve! {:watch-paths ["src/wikiart.clj"]})

  ;; either call `clerk/show!` explicitly to show a given notebook, or use the File Watcher described below.
  (clerk/show! "src/wikiart.clj")

  ,)

