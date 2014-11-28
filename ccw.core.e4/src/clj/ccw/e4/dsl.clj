(ns ccw.e4.dsl
  "Eclipse 4 DSL namespace.
   You can consider using (:require [ccw.e4.dsl :refer-all])
   since the macros / functions exposed have been carefully chosen."
  (:require [ccw.e4.model :as m]
            [ccw.eclipse  :as e])
  (:import [ccw.util GenericHandler]))

(def ^:dynamic *load-key* "repl")

(defn handler-factory [context nsname name] 
  (GenericHandler. (str nsname "/" name)))

(defmacro defcommand [command command-name & {:as opts}]
  `(def ~command
     (let [spec# (-> (merge
                       {:id ~(str *ns* "/" command)
                        :name ~command-name}
                       ~opts)
                   (update-in [:transient-data]
                              assoc
                              "ccw/load-key" *load-key*)
                   (update-in [:tags]
                              (fnil conj #{})
                              "ccw"))]
       ;(println "spec#:" spec#)
       (m/merge-command! @m/app spec#))))

;;; TODO register for deletion in  m/*elements* (a set)
(defmacro defhandler
  "Defines a handler for the command. Takes a single additional argument
   that must be either a var symbol, or a closure that will be automatically
   bound to a var."
  [command var-symbol-or-closure]
  (let [cmd-id (str *ns* "/" command)
        id (str cmd-id "-handler")
        create-var? (not (symbol? var-symbol-or-closure))
        var-symbol (if create-var?
                     (gensym id)
                     var-symbol-or-closure)
        platform-uri (str "bundleclass://ccw.core/clojure/ccw.e4.dsl/handler-factory/"
                       (or (namespace var-symbol) (ns-name *ns*)) "/" (name var-symbol))]
    `(do
       ~(when create-var?
          `(def ~var-symbol
             ~(str "Var generated by CCW to hold the closure " var-symbol-or-closure)
             ~var-symbol-or-closure))
       (let [spec# (-> {:contribution-URI ~platform-uri
                        :command ~cmd-id
                        :id ~id}
                     (update-in [:transient-data]
                       assoc
                       "ccw/load-key" *load-key*)
                     (update-in [:tags]
                       (fnil conj #{})
                       "ccw"))]
         ;(println "spec#:" spec#)
         (m/merge-handler! @m/app spec#)))))

;; TODO support options !!!
(defmacro defkeybinding 
  "Example:
   (defkeybinding greeter \"Ctrl+Alt+M\")
   (defkeybinding greeter \"Ctrl+Alt+M\"
     :scheme :emacs

   :scheme
   Predefined values 

 ... TODO document all this carefully ...

:sequence
\"M1+M2+P\"
\"Ctrl+Alt+M\"

:scheme key - see ccw.e4.model/key-binding-scheme

:context key - see ccw.e4.model/key-binding-context

:command

:platform ; Based on SWT/getPlatform ()
:win32
:gtk
:motif
:carbon
:photon

:locale
:en
:en_CA

"
  [command key-sequence & {:as opts}]
  `(let [spec# (-> (merge {:command ~command
                           :scheme  :default
                           :key-sequence ~key-sequence
                           :context :window}
                          ~opts)
                 (update-in [:transient-data]
                              assoc
                              "ccw/load-key" *load-key*)
                   (update-in [:tags]
                              (fnil conj #{})
                              "ccw"))]
     ;(println "spec#:" spec#)
     (m/merge-key-binding! @m/app spec#)))
