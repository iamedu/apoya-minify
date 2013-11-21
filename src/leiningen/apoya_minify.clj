(ns leiningen.apoya-minify
  (:import [com.google.javascript.jscomp SourceFile CompilerOptions]))

(defn compile-bundle [sources]
  (let [c (com.google.javascript.jscomp.Compiler. System/err)
        externs []
        source-files (map #(SourceFile/fromFile %) sources)
        opts (CompilerOptions.)] 
    (.compile c externs source-files opts)
    (.toSource c)))

(defn lookup-js [init-file prefix]
  (let [reg #"\"([\w\./-]+)\""
        file-contents (slurp init-file)
        files (re-seq reg file-contents)
        files (for [[_ f] files] f)]
    (filter #(.startsWith % prefix) files)))

(defn lookup-html [html-file prefix]
  (let [reg #"<script src=\"([\w\./-]+)\"></script>"
        file-contents (slurp html-file)
        files (re-seq reg file-contents)
        files (for [[_ f] files] f)]
    (filter #(.startsWith % prefix) files)))

(defn apoya-minify
  "Minify apoya's dependencies"
  [{{:keys [init-file html-file prefix source-dir output-dir]
     :or {prefix "bower_components"}} :minify} & args]
  (let [js-files (lookup-js init-file prefix)
        html-files (lookup-html html-file prefix)
        files (concat js-files html-files)]
    (doseq [f files]
      (let [compiled (if (.endsWith f ".js")
                       (compile-bundle [(str source-dir "/" f)])
                       (slurp (str source-dir "/" f)))
            output-file (java.io.File. (str output-dir "/" f))]
        (.mkdirs (.getParentFile output-file))
        (spit output-file compiled)))))
