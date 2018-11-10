(ns tech.ml.dataset-test
  (:require [clojure.test :refer :all]
            [tech.ml.dataset :as dataset]
            [tech.verify.ml.classification :as vf-classify]))


(defn- vectorize-result
  [coalesced-ds]
  (->> coalesced-ds
       (mapv (fn [ds-entry]
               (->> ds-entry
                    (map (fn [[k v]]
                           [k (if (number? v)
                                (long v)
                                (mapv long v))]))
                    (into {}))))))


(defn- make-test-ds
  [& [num-items]]
  (->> (range)
       (partition 4)
       (map (fn [item-seq]
              {:a (take 2 item-seq)
               :b (nth item-seq 2)
               :c (last item-seq)}))
       (take (or num-items 10))))


(deftest dataset-base
  (let [test-ds (make-test-ds)]
    (testing "basic coalescing"
      (let [correct [{:values [0 1 2], :label [3]}
                     {:values [4 5 6], :label [7]}
                     {:values [8 9 10], :label [11]}
                     {:values [12 13 14], :label [15]}
                     {:values [16 17 18], :label [19]}
                     {:values [20 21 22], :label [23]}
                     {:values [24 25 26], :label [27]}
                     {:values [28 29 30], :label [31]}
                     {:values [32 33 34], :label [35]}
                     {:values [36 37 38], :label [39]}]
            correct-scalar-label
            [{:values [0 1 2], :label 3}
             {:values [4 5 6], :label 7}
             {:values [8 9 10], :label 11}
             {:values [12 13 14], :label 15}
             {:values [16 17 18], :label 19}
             {:values [20 21 22], :label 23}
             {:values [24 25 26], :label 27}
             {:values [28 29 30], :label 31}
             {:values [32 33 34], :label 35}
             {:values [36 37 38], :label 39}]
            correct-no-label [{:values [0 1 2]}
                              {:values [4 5 6]}
                              {:values [8 9 10]}
                              {:values [12 13 14]}
                              {:values [16 17 18]}
                              {:values [20 21 22]}
                              {:values [24 25 26]}
                              {:values [28 29 30]}
                              {:values [32 33 34]}
                              {:values [36 37 38]}]]
        (is (= correct
               (->> (dataset/coalesce-dataset
                     [:a :b] :c {} test-ds)
                    vectorize-result)))
        (is (= correct
               (->> (dataset/coalesce-dataset
                     [:a :b] :c {:batch-size 1} test-ds)
                    vectorize-result)))
        (is (= correct-no-label
               (->> (dataset/coalesce-dataset
                     [:a :b] nil {:keep-extra? false} test-ds)
                    vectorize-result)))))
    (testing "batch coalescing"
      (let [correct [{:values [0 1 2 4 5 6], :label [3 7]}
                     {:values [8 9 10 12 13 14], :label [11 15]}
                     {:values [16 17 18 20 21 22], :label [19 23]}
                     {:values [24 25 26 28 29 30], :label [27 31]}
                     {:values [32 33 34 36 37 38], :label [35 39]}]]
          (is (= correct
                 (->> (dataset/coalesce-dataset
                       [:a :b] :c {:batch-size 2} test-ds)
                      vectorize-result)))))))


(deftest test-categorical-data
  (let [test-ds (vf-classify/fruit-dataset)
        {:keys [coalesced-dataset options]}
        (dataset/apply-dataset-options [:color-score :height :mass :width] :fruit-name
                                       {:deterministic-label-map? true
                                        :multiclass-label-base-index 1}
                                       test-ds)]
    (is (= options {:label-map {:fruit-name {:apple 1 :lemon 4
                                             :mandarin 2 :orange 3}}
                    :deterministic-label-map? true
                    :multiclass-label-base-index 1}))
    (is (= [{:values [0 7 192 8], :label [1]}
	   {:values [0 6 180 8], :label [1]}
	   {:values [0 7 176 7], :label [1]}
	   {:values [0 4 86 6], :label [2]}
	   {:values [0 4 84 6], :label [2]}]
           (->> coalesced-dataset
                vectorize-result
                (take 5)
                vec)))
    (let [{:keys [coalesced-dataset]}
          (dataset/apply-dataset-options [:color-score :height :mass :width] :fruit-name
                                         {:label-map {:fruit-name {:apple 4 :lemon 2
                                                                   :mandarin 3 :orange 1}}}
                                         test-ds)]
      (is (= [{:values [0 7 192 8], :label [4]}
              {:values [0 6 180 8], :label [4]}
              {:values [0 7 176 7], :label [4]}
              {:values [0 4 86 6], :label [3]}
              {:values [0 4 84 6], :label [3]}]
             (->> coalesced-dataset
                  vectorize-result
                  (take 5)
                  vec))))
    (let [{:keys [options coalesced-dataset]}
          (dataset/apply-dataset-options [:color-score :height :mass :width :fruit-subtype]
                                         :fruit-name
                                         {:deterministic-label-map? true
                                          :multiclass-label-base-index 1} test-ds)]
      (is (= {:label-map
              {:fruit-subtype
               {:golden-delicious 4
                :unknown 10
                :granny-smith 1
                :braeburn 3
                :spanish-jumbo 6
                :selected-seconds 7
                :mandarin 2
                :cripps-pink 5
                :turkey-navel 8
                :spanish-belsan 9}
               :fruit-name {:apple 1 :mandarin 2
                            :orange 3 :lemon 4}}
              :deterministic-label-map? true
              :multiclass-label-base-index 1}
             options))
      (is (= [{:values [0 7 192 8 1], :label [1]}
              {:values [0 6 180 8 1], :label [1]}
              {:values [0 7 176 7 1], :label [1]}
              {:values [0 4 86 6 2], :label [2]}
              {:values [0 4 84 6 2], :label [2]}]
             (->> coalesced-dataset
                  vectorize-result
                  (take 5)
                  vec))))))
