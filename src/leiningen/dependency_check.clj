(ns leiningen.dependency-check
  (:require [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [leiningen.core.classpath :as cp]
            [leiningen.core.eval :as eval]))

(def cli-options
  "Command line options accepts:
  log, throw, output-format, output-directory, properties-file."
  [[nil "--log" "Log to stdout"]
   [nil "--throw" "throw error when vulnerabilities found"]
   ["-p" "--properties-file FILE" "Specificies a file that contains properties to merge with defaults."]
   ["-f" "--output-format FORMAT(S)" "The output format to write to (XML, HTML, CSV, JSON, VULN, ALL). Default is HTML"
    :default ["html"]
    :parse-fn (fn [output-format]
                (-> (string/replace output-format #":" "")
                    (string/split #",")))]
   ["-o" "--output-directory DIR" "The folder to write to. The default is ./target"
    :default "target"]])

(defn- dependency-check-project
  "Create a project to launch dependency-check, with only dependency-check as a dependency."
  [project]
  (if-let [dependency-check-vec (first
                                 (drop-while
                                  (complement
                                   (fn [v] (= (first v) 'com.livingsocial/lein-dependency-check)))
                                  (:plugins project)))]
    {:dependencies [dependency-check-vec]}
    (throw (Exception. (str "dependency-check should be in your :plugins vector, "
                            "either in your ~/.lein/profiles.clj or in "
                            "the project itself.")))))

(defn dependency-check
  [project & args]
  (let [classpath (cp/get-classpath project)
        name (:name project)
        config (merge (:dependency-check project)
                      (:options (parse-opts args cli-options)))]
    (eval/eval-in-project (dependency-check-project project)
                          `(lein-dependency-check.core/main '~classpath '~name '~config)
                          '(require 'lein-dependency-check.core))))
