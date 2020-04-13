;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software

(ns cljs.compiler.api
  "This is intended to be a stable api for those who need programmatic access
  to the compiler."
  (:refer-clojure :exclude [munge])
  (:require [cljs.analyzer :as ana]
            [cljs.analyzer.api :as ana-api]
            [cljs.compiler :as comp]
            [cljs.closure :as closure]))

;; =============================================================================
;; Main API

(defn munge
  "Munge a symbol or string. Preserves the original type."
  [s]
  (comp/munge s))

(defn emit
  "Given an AST node generated by the analyzer emit JavaScript as a string."
  ([ast]
   (emit (ana-api/empty-state) ast))
  ([state ast]
   (ana-api/with-state state
     (with-out-str
       (comp/emit ast)))))

(defn with-core-cljs
  "Ensure that core.cljs has been loaded."
  ([]
   (comp/with-core-cljs
     (when-let [state (ana-api/current-state)]
       (:options @state))))
  ([opts] (with-core-cljs opts (fn [])))
  ([opts body]
   (with-core-cljs (ana-api/empty-state opts) opts body))
  ([state opts body]
   (ana-api/with-state state
     (binding [ana/*cljs-warning-handlers* (:warning-handlers opts ana/*cljs-warning-handlers*)]
       (comp/with-core-cljs opts body)))))

(defn requires-compilation?
  "Return true if the src file requires compilation."
  ([src dest] (requires-compilation? src dest nil))
  ([src dest opts]
   (requires-compilation? (ana-api/empty-state opts) src dest opts))
  ([state src dest opts]
   (ana-api/with-state state
     (binding [ana/*cljs-warning-handlers* (:warning-handlers opts ana/*cljs-warning-handlers*)]
       (comp/requires-compilation? src dest opts)))))

(defn compile-file
  "Compiles src to a file of the same name, but with a .js extension,
   in the src file's directory.

   With dest argument, write file to provided location. If the dest
   argument is a file outside the source tree, missing parent
   directories will be created. The src file will only be compiled if
   the dest file has an older modification time.

   Both src and dest may be either a String or a File.

   Returns a map containing {:ns .. :provides .. :requires .. :file ..}.
   If the file was not compiled returns only {:file ...}"
  ([src]
   (compile-file src (closure/src-file->target-file src)))
  ([src dest]
   (compile-file src dest nil))
  ([src dest opts]
   (compile-file (ana-api/empty-state opts) src dest opts))
  ([state src dest opts]
   (ana-api/with-state state
     (binding [ana/*cljs-warning-handlers* (:warning-handlers opts ana/*cljs-warning-handlers*)]
       (comp/compile-file src dest opts)))))

(defn cljs-files-in
  "Return a sequence of all .cljs and .cljc files in the given directory."
  [dir]
  (comp/cljs-files-in dir))

(defn compile-root
  "Looks recursively in src-dir for .cljs files and compiles them to
   .js files. If target-dir is provided, output will go into this
   directory mirroring the source directory structure. Returns a list
   of maps containing information about each file which was compiled
   in dependency order."
  ([src-dir] (compile-root src-dir "out"))
  ([src-dir target-dir] (compile-root src-dir target-dir nil))
  ([src-dir target-dir opts]
   (compile-root (ana-api/empty-state opts) src-dir target-dir opts))
  ([state src-dir target-dir opts]
   (ana-api/with-state state
     (binding [ana/*cljs-warning-handlers* (:warning-handlers opts ana/*cljs-warning-handlers*)]
       (comp/compile-root src-dir target-dir opts)))))
