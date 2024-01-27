# clip.clj

CLIP Embeddings for images and text. A clojure wrapper for [clip.cpp](https://github.com/monatis/clip.cpp).

## Dependency

For llama.clj with required native dependencies:

```clojure
com.phronemophobic/clip-clj {:mvn/version "1.0"}
;; native deps
com.phronemophobic.cljonda/clip-cpp-linux-x86-64 {:mvn/version "45cc1cab442b3bb019dadce8dcba04d76e99e9c3"}
com.phronemophobic.cljonda/clip-cpp-darwin-aarch64 {:mvn/version "45cc1cab442b3bb019dadce8dcba04d76e99e9c3"}
com.phronemophobic.cljonda/clip-cpp-darwin-x86-64 {:mvn/version "45cc1cab442b3bb019dadce8dcba04d76e99e9c3"}
```

## Quick Start

If you're just looking for a model to try things out, try the 3.6Gb [llama2 7B chat model](https://huggingface.co/TheBloke/Llama-2-7B-Chat-GGML/tree/main)  from TheBloke. Make sure to check the link for important info like license and use policy.

Download a model:

```sh
mkdir -p models
(cd models && curl -L -O 'https://huggingface.co/mys/ggml_CLIP-ViT-B-32-laion2B-s34B-b79K/resolve/main/CLIP-ViT-B-32-laion2B-s34B-b79K_ggml-model-f16.gguf')
```

```clojure

(require '[com.phronemophobic.clip :as clip])

(def ctx (clip/create-context "models/CLIP-ViT-B-32-laion2B-s34B-b79K_ggml-model-f16.gguf"))

(clip/cosine-similarity
 (clip/text-embedding ctx "apple")
 (clip/text-embedding ctx "banana"))

(clip/cosine-similarity
 (clip/image-embedding ctx "path/to/image.png")
 (clip/text-embedding ctx "green"))


```

See https://github.com/monatis/clip.cpp for more info on models and usage.

## License

 The MIT License (MIT)

Copyright © 2024 Adrian Smith

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.


