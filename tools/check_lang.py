"""Compare every language file with en_us.json.

The language files live in common/src/main/resources/assets/whathitme/lang/*.json (the single source; the
.lang files of 1.12.2 and older are generated from them at build time).

Lists keys that are missing or extra in each language, keys whose format
placeholders (%s, %d, %1$s, %%) differ, and keys out of en_us order.
Exit code 1 when anything is missing, extra or mismatched.

    python tools/check_lang.py
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LANG_DIR = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "whathitme", "lang")
LANGS = ["ja_jp", "zh_cn", "zh_tw", "ko_kr", "ru_ru", "de_de", "fr_fr", "es_es", "pt_br"]
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[sd]|%%")


def read(path):
    import json
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    return dict(data), list(data.keys())


def main():
    en, en_order = read(os.path.join(LANG_DIR, "en_us.json"))
    problems = 0
    for lang in LANGS:
        path = os.path.join(LANG_DIR, lang + ".json")
        if not os.path.exists(path):
            print("%s: file missing" % lang)
            problems += 1
            continue
        tr, order = read(path)
        missing = [k for k in en_order if k not in tr]
        extra = [k for k in order if k not in en]
        bad = [k for k in en_order if k in tr and sorted(PLACEHOLDER.findall(en[k])) != sorted(PLACEHOLDER.findall(tr[k]))]
        common = [k for k in order if k in en]
        expected = [k for k in en_order if k in tr]
        if missing or extra or bad:
            problems += 1
        for k in missing:
            print("%s: missing %s" % (lang, k))
        for k in extra:
            print("%s: extra   %s" % (lang, k))
        for k in bad:
            print("%s: placeholders differ in %s" % (lang, k))
        if common != expected:
            print("%s: key order differs from en_us.json (warning)" % lang)
    if problems == 0:
        print("OK: %d keys, %d languages, nothing missing or extra." % (len(en_order), len(LANGS)))
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
