#!/usr/bin/env python3
"""Checks that every quotation in Quotes.kt appears word for word in H. Rackham's
translation of the Nicomachean Ethics, in the book and chapter it cites, and
that the cited Bekker page is right.

    python3 tools/verify_quotes.py

Downloads the Perseus Digital Library text (CC BY-SA 4.0) from GitHub.
"""
import re
import sys
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

SOURCE = ("https://raw.githubusercontent.com/PerseusDL/canonical-greekLit/master/"
          "data/tlg0086/tlg010/tlg0086.tlg010.perseus-eng2.xml")
QUOTES = Path(__file__).resolve().parent.parent / "app/src/main/java/com/stagirite/ledger/data/Quotes.kt"
TEI = "{http://www.tei-c.org/ns/1.0}"
XML_BASE = "{http://www.w3.org/XML/1998/namespace}base"
ROMAN = {"I": 1, "II": 2, "III": 3, "IV": 4, "V": 5, "VI": 6, "VII": 7, "VIII": 8, "IX": 9, "X": 10}


def sections(root):
    """Yields (book.chapter.section, [(bekker page, text), ...]) with Rackham's notes removed."""
    page = None
    out = []

    def walk(el, pieces):
        nonlocal page
        tag = el.tag.replace(TEI, "")
        if tag == "note":
            pass
        elif tag == "milestone":
            if el.get("unit") == "page":
                page = el.get("n")
        elif tag == "div" and el.get("subtype") == "subsection":
            mine = []
            if el.text:
                mine.append((page, el.text))
            for child in el:
                walk(child, mine)
            out.append((el.get(XML_BASE).split(":")[-1] + "." + el.get("n"), mine))
        else:
            if el.text:
                pieces.append((page, el.text))
            for child in el:
                walk(child, pieces)
        if el.tail:
            pieces.append((page, el.tail))

    walk(root, [])
    return out


def main():
    with urllib.request.urlopen(SOURCE) as response:
        root = ET.fromstring(response.read())
    secs = sections(root)
    src = QUOTES.read_text(encoding="utf-8")
    quotes = re.findall(r'Quote\(\s*((?:"(?:[^"\\]|\\.)*"\s*\+?\s*)+),\s*"([^"]+)"', src)
    failures = 0
    for parts, ref in quotes:
        raw = "".join(re.findall(r'"((?:[^"\\]|\\.)*)"', parts))
        # Read the string as Kotlin will: decode \uXXXX escapes.
        text = re.sub(r"\\u([0-9A-Fa-f]{4})", lambda m: chr(int(m.group(1), 16)), raw).strip("…")
        book, rest = ref.split(".", 1)
        chapter, page = rest.split(", ")
        prefix = f"{ROMAN[book]}.{chapter}."
        found = None
        for name, pieces in secs:
            if not name.startswith(prefix):
                continue
            joined = ""
            starts = []
            for pg, piece in pieces:
                starts.append((len(joined), pg))
                joined += piece
            joined_norm = re.sub(r"\s+", " ", joined)
            if text in joined_norm:
                # Page in force where the quotation starts.
                at = joined_norm.index(text)
                norm_starts = [(len(re.sub(r"\s+", " ", joined[:i])), pg) for i, pg in starts]
                found = (name, [pg for i, pg in norm_starts if i <= at][-1])
                break
        if found is None:
            failures += 1
            print(f"FAIL  {ref}: not found word for word: {text[:60]}")
        elif found[1] != page:
            failures += 1
            print(f"FAIL  {ref}: found in {found[0]} but on page {found[1]}")
        else:
            print(f"ok    {ref:14} {found[0]:8} {text[:60]}")
    print(f"{len(quotes) - failures}/{len(quotes)} verified")
    sys.exit(1 if failures or not quotes else 0)


if __name__ == "__main__":
    main()
