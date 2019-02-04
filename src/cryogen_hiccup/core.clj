(ns cryogen-hiccup.core
  (:require [cryogen-core.markup :refer [markup-registry rewrite-hrefs]]
            [hiccup.core :as hiccup]
            [hiccup.compiler :as compiler]
            [clojure.string :as s])
  (:import cryogen_core.markup.Markup))

(defn rewrite-hrefs-transformer
  "A :replacement-transformer for use in markdown.core that will inject the
  given blog prefix in front of local links."
  [{:keys [blog-prefix]} text state]
  [(rewrite-hrefs blog-prefix text) state])

(defn apply-header-anchors
  [[tag attrs title :as form]]
  (if (contains? #{:h1 :h2 :h3 :h4 :h5 :h6} tag)
    [tag (update-in attrs [:id]
                    (fn [id]
                      (str (cond-> id (not-empty id) (str " "))
                           (-> title s/lower-case (s/replace " " "&#95;")))))
     title]
    form))

(defn hiccup
  []
  (reify Markup
    (dir [this] "hiccup")
    (ext [this] ".edn")
    (render-fn [this]
      (fn [rdr config]
        (->> (loop [html ""]
               (if-let [form (read rdr false nil)]
                 (recur (str html (-> (compiler/normalize-element form)
                                      apply-header-anchors
                                      hiccup/html)))
                 html))
             (rewrite-hrefs (:blog-prefix config)))))))

(defn init []
  (swap! markup-registry conj (hiccup)))
