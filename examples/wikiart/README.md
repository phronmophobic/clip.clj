


## Get the data

_Note: Be forewarned that artists love to depict the human form in all its glory. Some images may be nsfw._

```sh
# Make sure you have git-lfs installed (https://git-lfs.com)
cd data/

git lfs install
git clone https://huggingface.co/datasets/huggan/wikiart

```

## Get the clip model

See https://github.com/monatis/clip.cpp for more info on models and usage.

```sh
mkdir -p models
(cd models && curl -L -O 'https://huggingface.co/mys/ggml_CLIP-ViT-B-32-laion2B-s34B-b79K/resolve/main/CLIP-ViT-B-32-laion2B-s34B-b79K_ggml-model-f16.gguf')
```


## Index the data

```sh
# This took over 2 hours on my computer.
# It also requires an extra 32gb on top of the already downloaded models and data.
# Almost all of the extra disk space is creating an indexed copy of the wikiart image data.
# which isn't strictly necessary, but really speeds things up.
clojure -X:project wikiart.index/index-all
```

