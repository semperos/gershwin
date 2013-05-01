;;; gershwin-mode.el --- Major mode for Gershwin code

;; Copyright (C) 2013 Daniel Gregoire
;;
;; Authors: Daniel Gregoire <daniel.l.gregoire@gmail.com>
;; URL: https://github.com/semperos
;; Version: 0.1
;; Keywords: languages, lisp, stack-based, concatenative

;; This file is not part of GNU Emacs.

;;; Commentary:

;; This mode is derived from Clojure mode, with extra support
;; added for the Gershwin programming language.

;; Gershwin is implemented in Clojure and provides Clojure interop,
;; and since Gershwin is a concatenative language with a minimal
;; syntax of its own, Clojure mode serves the majority of its needs.

;;; Installation:

;; This mode requires:
;;
;;   * clojure-mode
;;   * paredit-mode (@todo Make this optional)
;;
;; Make sure they're installed first.

;; Then just add the following to your Emacs initialization:
;;
;; (require 'gershwin-mode)

(defvar gershwin-builtins
  '("swap" "dup" "dup2" "dup3" "drop" "drop2" "drop3" "nip" "nip2" "rot"
    "over" "over2" "pick" "dip" "dip2" "dip3" "dip4" "dupd" "keep" "keep2" "keep3"
    "bi" "bi2" "bi3" "tri" "tri2" "tri3" "bi*" "bi2*" "tri*" "tri2*" "?" "or" "and"
    "boolean" "bi&" "bi2&" "tri&" "tri2&" "both?" "either?")
  "Words built into the core of Gershwin")

;;;###autoload
(define-derived-mode gershwin-mode
  clojure-mode "Gershwin"
  "Major mode for Gershwin code"

  (cl-flet ((star-fn (item) (replace-regexp-in-string "\\*" "\\\\*" item))
            (qmark-fn (item) (replace-regexp-in-string "\\?" "\\\\?" item)))
    (let ((groomed-words (mapcar (lambda (item) (qmark-fn (star-fn item)))
                                 gershwin-builtins)))
      (font-lock-add-keywords 'gershwin-mode
                              (append '(": ")
                                      (mapcar (lambda (item)
                                                (concat "\\b" item "\\b"))
                                              groomed-words)))))

  (add-hook 'gershwin-mode-hook (lambda () (local-set-key (kbd "C-c a") 'paredit-wrap-angled)))
  (add-hook 'gershwin-mode-hook (lambda ()
                                  (modify-syntax-entry ?< "(>" )
                                  (modify-syntax-entry ?> ")<" ))))

;;;###autoload
(add-to-list 'auto-mode-alist '("\\.gwn$" . gershwin-mode))

(provide 'gershwin-mode)
