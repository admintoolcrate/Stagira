#!/usr/bin/env python3
"""
Draws a ToolCrate app's Play icon (512x512) and feature graphic (1024x500) in the family
look (docs/family-look.md): the launcher mark in white on #0F0F0F, the name in EB Garamond,
a letter-spaced Inter label and a one-sentence Inter tagline, laid out as Stagira's.

    python3 tools/family_graphics.py APP_NAME "LABEL" "Tagline." FONT_DIR OUT_DIR MARK.xml

MARK.xml is the app's launcher foreground vector (108x108 viewport). Only M/L/C/S/A/Z path
commands (absolute or relative) are understood, which is all the family marks use.
"""
import math, re, sys
from PIL import Image, ImageDraw, ImageFont
import xml.etree.ElementTree as ET

GROUND = (15, 15, 15)
MARK = (250, 250, 248)
NAME = (242, 242, 240)
GREY = (154, 154, 151)
SS = 4  # supersampling

def tokens(d):
    return re.findall(r"[MmLlCcSsAaZz]|-?\d*\.?\d+(?:e-?\d+)?", d)

def arc_points(x1, y1, rx, ry, phi, large, sweep, x2, y2, n=180):
    # SVG elliptical arc → points (endpoint to centre parameterisation), circles only here.
    phi = math.radians(phi)
    dx, dy = (x1 - x2) / 2, (y1 - y2) / 2
    x1p = math.cos(phi) * dx + math.sin(phi) * dy
    y1p = -math.sin(phi) * dx + math.cos(phi) * dy
    lam = (x1p ** 2) / rx ** 2 + (y1p ** 2) / ry ** 2
    if lam > 1: rx *= math.sqrt(lam); ry *= math.sqrt(lam)
    num = rx * rx * ry * ry - rx * rx * y1p * y1p - ry * ry * x1p * x1p
    den = rx * rx * y1p * y1p + ry * ry * x1p * x1p
    co = math.sqrt(max(0, num / den)) * (-1 if large == sweep else 1)
    cxp, cyp = co * rx * y1p / ry, -co * ry * x1p / rx
    cx = math.cos(phi) * cxp - math.sin(phi) * cyp + (x1 + x2) / 2
    cy = math.sin(phi) * cxp + math.cos(phi) * cyp + (y1 + y2) / 2
    def ang(ux, uy, vx, vy):
        a = math.atan2(ux * vy - uy * vx, ux * vx + uy * vy); return a
    t1 = ang(1, 0, (x1p - cxp) / rx, (y1p - cyp) / ry)
    dt = ang((x1p - cxp) / rx, (y1p - cyp) / ry, (-x1p - cxp) / rx, (-y1p - cyp) / ry)
    if not sweep and dt > 0: dt -= 2 * math.pi
    if sweep and dt < 0: dt += 2 * math.pi
    pts = []
    for i in range(1, n + 1):
        t = t1 + dt * i / n
        x, y = rx * math.cos(t), ry * math.sin(t)
        pts.append((math.cos(phi) * x - math.sin(phi) * y + cx, math.sin(phi) * x + math.cos(phi) * y + cy))
    return pts

def path_polylines(d):
    t = tokens(d); i = 0; cmd = None; cur = (0, 0); start = (0, 0); last_c2 = None
    lines = []; poly = []
    def num():
        nonlocal i; v = float(t[i]); i += 1; return v
    while i < len(t):
        if re.match(r"[A-Za-z]", t[i]): cmd = t[i]; i += 1
        rel = cmd.islower(); c = cmd.upper()
        ox, oy = cur if rel else (0, 0)
        if c == 'M':
            if poly: lines.append(poly)
            cur = (num() + ox, num() + oy); start = cur; poly = [cur]; cmd = 'l' if rel else 'L'; last_c2 = None
        elif c == 'L':
            cur = (num() + ox, num() + oy); poly.append(cur); last_c2 = None
        elif c in 'CS':
            if c == 'C':
                c1 = (num() + ox, num() + oy)
            else:
                c1 = (2 * cur[0] - last_c2[0], 2 * cur[1] - last_c2[1]) if last_c2 else cur
            c2 = (num() + ox, num() + oy); p = (num() + ox, num() + oy)
            for k in range(1, 61):
                s = k / 60
                x = (1-s)**3*cur[0] + 3*(1-s)**2*s*c1[0] + 3*(1-s)*s*s*c2[0] + s**3*p[0]
                y = (1-s)**3*cur[1] + 3*(1-s)**2*s*c1[1] + 3*(1-s)*s*s*c2[1] + s**3*p[1]
                poly.append((x, y))
            last_c2 = c2; cur = p
        elif c == 'A':
            rx, ry, phi, large, sweep = num(), num(), num(), int(num()), int(num())
            p = (num() + ox, num() + oy)
            poly += arc_points(cur[0], cur[1], rx, ry, phi, large, sweep, p[0], p[1]); cur = p; last_c2 = None
        elif c == 'Z':
            poly.append(start); cur = start; last_c2 = None
    if poly: lines.append(poly)
    return lines

def load_mark(xml_path):
    ns = '{http://schemas.android.com/apk/res/android}'
    out = []
    for el in ET.parse(xml_path).getroot().iter('path'):
        d = el.get(ns + 'pathData'); w = float(el.get(ns + 'strokeWidth', '0') or 0)
        filled = (el.get(ns + 'fillColor') or '#00000000')[-8:-6] not in ('00',) and w == 0
        out.append((path_polylines(d), w, filled))
    return out

def draw_mark(draw, mark, cx, cy, scale):
    for lines, w, filled in mark:
        for poly in lines:
            pts = [(cx + (x - 54) * scale, cy + (y - 54) * scale) for x, y in poly]
            if filled:
                draw.polygon(pts, fill=MARK); continue
            r = w * scale / 2
            for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
                n = max(1, int(math.hypot(x1 - x0, y1 - y0) / (r / 3)))
                for k in range(n + 1):
                    x = x0 + (x1 - x0) * k / n; y = y0 + (y1 - y0) * k / n
                    draw.ellipse([x - r, y - r, x + r, y + r], fill=MARK)

def icon(mark, out):
    im = Image.new('RGB', (512 * SS, 512 * SS), GROUND); d = ImageDraw.Draw(im)
    draw_mark(d, mark, 256 * SS, 256 * SS, 6.85 * SS)
    im.resize((512, 512), Image.LANCZOS).save(out)

def spaced(draw, xy, text, font, fill, tracking):
    x, y = xy
    for ch in text:
        draw.text((x, y), ch, font=font, fill=fill)
        x += font.getlength(ch) + tracking

def feature(mark, name, label, tagline, fonts, out):
    W, H = 1024 * SS, 500 * SS
    im = Image.new('RGB', (W, H), GROUND); d = ImageDraw.Draw(im)
    draw_mark(d, mark, 186 * SS, 250 * SS, 4.3 * SS)
    g = ImageFont.truetype(fonts + '/eb_garamond_regular.ttf', int(116 * SS))
    d.text((474 * SS - g.getbbox(name)[0], 149 * SS - g.getbbox('S')[1]), name, font=g, fill=NAME)
    lab = ImageFont.truetype(fonts + '/inter.ttf', int(18 * SS))
    lab.set_variation_by_axes([18, 500])
    spaced(d, (477 * SS, 289 * SS - lab.getbbox('T')[1]), label, lab, GREY, 0.3 * 18 * SS)
    tag = ImageFont.truetype(fonts + '/inter.ttf', int(27.5 * SS))
    tag.set_variation_by_axes([26, 400])
    d.text((478 * SS - tag.getbbox(tagline)[0], 326 * SS - tag.getbbox('C')[1] + 1 * SS), tagline, font=tag, fill=GREY)
    im.resize((1024, 500), Image.LANCZOS).save(out)

if __name__ == '__main__':
    name, label, tagline, fonts, outdir, mark_xml = sys.argv[1:7]
    m = load_mark(mark_xml)
    icon(m, outdir + '/icon-512.png')
    feature(m, name, label, tagline, fonts, outdir + '/feature-graphic-1024x500.png')
