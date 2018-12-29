(defproject reddit-viewer "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :min-lein-version "2.0.0"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[cljs-ajax "0.8.0"]
                 [cljsjs/chartjs "2.7.3-0"]
                 [funcool/cuerdas "2.0.5"]
                 [org.clojure/clojure "1.9.0" :scope "provided"]
                 [org.clojure/clojurescript "1.10.439" :scope "provided"]
                 [reagent "0.8.1"]
                 [re-frame "0.10.6"]]

  :plugins [[lein-cljsbuild "1.1.5"]
            [lein-figwheel "0.5.16"]]

  :clean-targets ^{:protect false}
[:target-path
 [:cljsbuild :builds :app :compiler :output-dir]
 [:cljsbuild :builds :app :compiler :output-to]]

  :resource-paths ["public"]

  :figwheel {:http-server-root "."
             :nrepl-port       7002
             :nrepl-middleware [cider.piggieback/wrap-cljs-repl]
             :css-dirs         ["public/css"]}

  :cljsbuild {:builds {:app
                       {:source-paths ["src" "env/dev/cljs"]
                        :compiler
                                      {:closure-defines {"re_frame.trace.trace_enabled_QMARK_" true}
                                       :preloads        [day8.re-frame-10x.preload]
                                       :main            "reddit-viewer.dev"
                                       :output-to       "public/js/app.js"
                                       :output-dir      "public/js/out"
                                       :asset-path      "js/out"
                                       :source-map      true
                                       :optimizations   :none
                                       :pretty-print    true}
                        :figwheel
                                      {:on-jsload "reddit-viewer.core/mount-root"
                                       :open-urls ["http://localhost:3449/index.html"]}}
                       :release
                       {:source-paths ["src" "env/prod/cljs"]
                        :compiler
                                      {:output-to     "public/js/app.js"
                                       :output-dir    "public/js/release"
                                       :asset-path    "js/out"
                                       :optimizations :advanced
                                       :pretty-print  false}}}}

  :aliases {"package" ["do" "clean" ["cljsbuild" "once" "release"]]}

  :profiles {:dev {:source-paths ["src" "env/dev/clj"]
                   :dependencies [[binaryage/devtools "0.9.10"]
                                  [figwheel-sidecar "0.5.16"]
                                  [nrepl "0.4.4"]
                                  [cider/piggieback "0.3.8"]
                                  [day8.re-frame/re-frame-10x "0.3.6-react16"]]}})
