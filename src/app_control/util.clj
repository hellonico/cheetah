(ns app-control.util
  (:require [noir.io :as io]
            [markdown.core :as md]))

(defn md->html
  "reads a markdown file from public/md and returns an HTML string"
  [filename]
  (md/md-to-html-string (io/slurp-resource filename)))

(defn replace-several
   "apply several string.replace in one function "
  [str & replacements]
  (reduce (fn [s [a b]]
            (clojure.string/replace s a b))
          str
          (partition 2 replacements)))