(ns com.phronemophobic.clip.raw
  (:require [com.phronemophobic.clong.gen.jna :as gen]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [com.rpl.specter :as specter])
  (:import java.lang.ref.Cleaner)
  (:gen-class))

(def cleaner (Cleaner/create))

(defn ^:private write-edn [w obj]
  (binding [*print-length* nil
            *print-level* nil
            *print-dup* false
            *print-meta* false
            *print-readably* true

            ;; namespaced maps not part of edn spec
            *print-namespace-maps* false

            *out* w]
    (pr obj)))

(def lib-options
  {com.sun.jna.Library/OPTION_STRING_ENCODING "UTF8"})
(def ^:no-doc lib-ggml
  (com.sun.jna.NativeLibrary/getInstance "ggml" lib-options))
(def ^:no-doc lib-clip
  (com.sun.jna.NativeLibrary/getInstance "clip" lib-options))

(def arguments
  [ "-resource-dir"
 "/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/lib/clang/15.0.0"
 "-isysroot"
 "/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk"
 "-I/usr/local/include"
 "-internal-isystem"
 "/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/local/include"
 "-internal-isystem"
 "/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/lib/clang/15.0.0/include"
 "-internal-externc-isystem"
 "/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include"
 "-internal-externc-isystem"
 "/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/include"])

(defn ^:private dump-api []
  (let [outf (io/file
              "resources"
              "com"
              "phronemophobic"
              "clip"
              "api.edn")]
    (.mkdirs (.getParentFile outf))
    (with-open [w (io/writer outf)]
      (write-edn w
                 ((requiring-resolve 'com.phronemophobic.clong.clang/easy-api)
                  "/Users/adrian/workspace/clip.cpp/clip.h"
                  arguments)))))


(def api
  ((requiring-resolve 'com.phronemophobic.clong.clang/easy-api) "/Users/adrian/workspace/clip.cpp/clip.h"
   #_(into arguments
         ["-I/Users/adrian/workspace/clip.cpp/"])) 
  #_(with-open [rdr (io/reader
                     (io/resource
                      "com/phronemophobic/tree-sitter/api.edn"))
                rdr (java.io.PushbackReader. rdr)]
      (edn/read rdr)))


(gen/def-api lib-clip api)

(let [struct-prefix (gen/ns-struct-prefix *ns*)]
  (defmacro import-structs! []
    `(gen/import-structs! api ~struct-prefix)))
