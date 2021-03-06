(in-ns 'test.core)

(deftest astrolabe-memory
  "Astrolabe - Gain 1 memory"
  (do-game
    (new-game (default-corp) (default-runner [(qty "Astrolabe" 3)]))
    (take-credits state :corp)
    (play-from-hand state :runner "Astrolabe")
    (is (= 5 (:memory (get-runner))) "Gain 1 memory")))

(deftest astrolabe-draw
  "Astrolabe - Draw on new server install"
  (do-game
    (new-game (default-corp [(qty "Snare!" 3) (qty "Shock!" 3) (qty "Project Junebug" 3)])
              (default-runner [(qty "Astrolabe" 3) (qty "Sure Gamble" 3) (qty "Cloak" 1)]))
    (take-credits state :corp)
    (play-from-hand state :runner "Astrolabe")
    (take-credits state :runner 3)
    ; corp's turn. install something from HQ to trigger Astrolabe draw
    (core/play state :corp {:card (first (:hand (get-corp))) :server "New remote"})
    (is (= 5 (count (:hand (get-runner)))) "Drew 1 card from server install")
    ; install over the old server; make sure nothing is drawn
    (core/play state :corp {:card (first (:hand (get-corp))) :server "Server 0"})
    (is (= 5 (count (:hand (get-runner)))) "Did not draw")
    (is (= 1 (count (:deck (get-runner)))) "1 card left in deck")))
  
(deftest turntable-swap
  "Turntable - Swap a stolen agenda for a scored agenda"
  (do-game
    (new-game (default-corp [(qty "Domestic Sleepers" 1) (qty "Project Vitruvius" 1)])
              (default-runner [(qty "Turntable" 1)]))
    (play-from-hand state :corp "Project Vitruvius" "New remote")
    (let [ag1 (get-in @state [:corp :servers :remote1 :content 0])]
      (core/gain state :corp :click 1)
      (core/advance state :corp {:card (refresh ag1)})
      (core/advance state :corp {:card (refresh ag1)})
      (core/advance state :corp {:card (refresh ag1)})
      (core/score state :corp {:card (refresh ag1)})
      (take-credits state :corp)
      (play-from-hand state :runner "Turntable")
      (is (= 3 (:credit (get-runner))))
      (let [tt (get-in @state [:runner :rig :hardware 0])]
        (core/click-run state :runner {:server "HQ"})
        (core/no-action state :corp nil)
        (core/successful-run state :runner nil)
        (prompt-choice :runner "Steal")
        (is (= 0 (:agenda-point (get-runner))) "Stole Domestic Sleepers")
        (is (= true (:swap (core/get-card state tt)))) ; Turntable ability enabled by steal
        (card-ability state :runner tt 0)
        ; Turntable prompt should be active
        (is (= (:cid tt) (get-in @state [:runner :prompt 0 :card :cid])))
        (prompt-choice :runner "Yes")
        (core/select state :runner {:card (find-card "Project Vitruvius" (:scored (get-corp)))})
        (is (= 2 (:agenda-point (get-runner))) "Took Project Vitruvius from Corp")
        (is (= 0 (:agenda-point (get-corp))) "Swapped Domestic Sleepers to Corp")
        (is (nil? (:swap (core/get-card state tt))) "Turntable ability disabled")))))
