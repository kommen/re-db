(ns re-db.fast
  (:refer-clojure :exclude [get-in]))

(defmacro get-in
  "Compiled version of get-in, faster than `get-in` function"
  [m ks]
  (if-not (vector? ks)
    `(clojure.core/get-in ~m ~ks)
    `(-> ~m
         ~@(for [k ks]
             (list 'clojure.core/get k)))))

(defmacro get-in-objs
  "Lookups in javascript objects, keywords converted to strings"
  [m ks]
  (assert (vector? ks))
  `(j/get-in ~m ~(mapv (fn [k] (if (keyword? k)
                                 (subs (str k) 1)
                                 `(j/!get ~k ~'.-fqn))) ks)))



(defmacro defmemo-1 [name fsym]
  `(let [cache# (volatile! {})]
     (defn ~name [x#]
       (if-some [res# (@cache# x#)]
         res#
         (let [res# (~fsym x#)]
             (vswap! cache# assoc x# res#)
             res#)))))

(defmacro if-found [[sym lookup-expr] then else]
  `(let [~sym ~(concat lookup-expr (list 're-db.fast/nf-sentinel))]
     (if (identical? ~sym ~'re-db.fast/nf-sentinel)
       ~else
       ~then)))

(defmacro invoke->
  "Like -> but calls each function using -invoke"
  {:added "1.0"}
  [x & forms]
  (loop [x x, forms forms]
    (if forms
      (let [form (first forms)
            threaded (if (seq? form)
                       (with-meta `(~'cljs.core/-invoke ~(first form) ~x ~@(next form)) (meta form))
                       (list form x))]
        (recur threaded (next forms)))
      x)))

(defmacro t2-update-in [m path f & args])

(defmacro update! [m k f & args]
  `(let [m# ~m
         k# ~k
         v# (m# k#)]
     (assoc! m# k# (~f v# ~@args))))