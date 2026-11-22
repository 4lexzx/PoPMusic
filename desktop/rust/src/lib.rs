use wasm_bindgen::prelude::*;

// ---------------------------------------------------------------------------
// LRC: réplica del comportamiento de src/shared/utils/lrc.ts
// ---------------------------------------------------------------------------

struct Word {
    time: f64,
    text: String,
}

struct Line {
    time: f64,
    end: f64,
    text: String,
    words: Vec<Word>,
}

/// Parsea `[mm:ss.xx]` / `[mm:ss:xx]` (o `<...>`) y devuelve (segundos, fin).
/// Equivale a `toSeconds` + los regex `LINE_TIME` / `WORD_TAG` del JS.
fn parse_stamp(bytes: &[u8], start: usize, open: u8, close: u8) -> Option<(f64, usize)> {
    if bytes.get(start) != Some(&open) {
        return None;
    }
    let mut i = start + 1;

    // Minutos: 1-3 dígitos.
    let min_start = i;
    while i < bytes.len() && bytes[i].is_ascii_digit() && i - min_start < 3 {
        i += 1;
    }
    if i == min_start {
        return None;
    }
    let minutes = std::str::from_utf8(&bytes[min_start..i])
        .ok()?
        .parse::<u32>()
        .ok()?;

    if bytes.get(i) != Some(&b':') {
        return None;
    }
    i += 1;

    // Segundos: 1-2 dígitos.
    let sec_start = i;
    while i < bytes.len() && bytes[i].is_ascii_digit() && i - sec_start < 2 {
        i += 1;
    }
    if i == sec_start {
        return None;
    }
    let seconds = std::str::from_utf8(&bytes[sec_start..i])
        .ok()?
        .parse::<u32>()
        .ok()?;

    // Fracción opcional de 1-3 dígitos: 1 => *100, 2 => *10, 3 => directo; /1000.
    let mut fraction: u32 = 0;
    if let Some(&sep) = bytes.get(i) {
        if sep == b'.' || sep == b':' {
            let frac_start = i + 1;
            let mut j = frac_start;
            while j < bytes.len() && bytes[j].is_ascii_digit() && j - frac_start < 3 {
                j += 1;
            }
            if j > frac_start {
                let raw = std::str::from_utf8(&bytes[frac_start..j]).ok()?;
                let value = raw.parse::<u32>().ok()?;
                fraction = match raw.len() {
                    1 => value * 100,
                    2 => value * 10,
                    _ => value,
                };
                i = j;
            }
        }
    }

    if bytes.get(i) != Some(&close) {
        return None;
    }
    Some((
        minutes as f64 * 60.0 + seconds as f64 + fraction as f64 / 1000.0,
        i + 1,
    ))
}

/// Extrae todas las marcas de línea de una línea y su contenido sin marcas.
fn line_stamps(raw: &str) -> (Vec<f64>, String) {
    let bytes = raw.as_bytes();
    let mut times = Vec::new();
    let mut content = String::new();
    let mut last = 0usize;
    let mut i = 0usize;
    while i < bytes.len() {
        if let Some((time, end)) = parse_stamp(bytes, i, b'[', b']') {
            if last < i {
                content.push_str(&raw[last..i]);
            }
            times.push(time);
            i = end;
            last = end;
        } else {
            i += 1;
        }
    }
    if last < bytes.len() {
        content.push_str(&raw[last..]);
    }
    (times, content)
}

enum Segment {
    Tag(f64),
    Text(String),
}

/// Divide el contenido por marcas de palabra, conservando los segmentos de texto.
fn split_word_tags(content: &str) -> Vec<Segment> {
    let bytes = content.as_bytes();
    let mut segments = Vec::new();
    let mut last = 0usize;
    let mut i = 0usize;
    while i < bytes.len() {
        if let Some((time, end)) = parse_stamp(bytes, i, b'<', b'>') {
            if last < i {
                segments.push(Segment::Text(content[last..i].to_string()));
            }
            segments.push(Segment::Tag(time));
            i = end;
            last = end;
        } else {
            i += 1;
        }
    }
    if last < bytes.len() {
        segments.push(Segment::Text(content[last..].to_string()));
    }
    segments
}

/// Devuelve (texto sin marcas de palabra, palabras con tiempo absoluto).
fn word_times(content: &str) -> (String, Vec<Word>) {
    let mut text = String::new();
    let mut words = Vec::new();
    let mut pending: Option<f64> = None;

    for segment in split_word_tags(content) {
        match segment {
            Segment::Tag(time) => pending = Some(time),
            Segment::Text(part) => {
                text.push_str(&part);
                if let Some(time) = pending {
                    if !part.trim().is_empty() {
                        words.push(Word { time, text: part });
                        pending = None;
                    }
                }
            }
        }
    }
    (text, words)
}

/// Calcula `end` e interpala palabras cuando la línea no tiene marcas.
fn interpolate(lines: &mut [Line]) {
    for index in 0..lines.len() {
        let time = lines[index].time;
        let end = if index + 1 < lines.len() {
            lines[index + 1].time
        } else {
            time + 4.0
        };
        lines[index].end = if end < time + 0.6 { time + 0.6 } else { end };

        if lines[index].words.is_empty() && !lines[index].text.is_empty() {
            let tokens: Vec<&str> = lines[index].text.split_whitespace().collect();
            let span = lines[index].end - time;
            let step = if tokens.is_empty() {
                0.0
            } else {
                span / tokens.len() as f64
            };
            lines[index].words = tokens
                .iter()
                .enumerate()
                .map(|(i, token)| Word {
                    time: time + step * i as f64,
                    text: (*token).to_string(),
                })
                .collect();
        }
    }
}

// ---------------------------------------------------------------------------
// JSON (sin dependencias extra)
// ---------------------------------------------------------------------------

fn push_json_string(out: &mut String, value: &str) {
    out.push('"');
    for ch in value.chars() {
        match ch {
            '"' => out.push_str("\\\""),
            '\\' => out.push_str("\\\\"),
            '\u{08}' => out.push_str("\\b"),
            '\u{0c}' => out.push_str("\\f"),
            '\n' => out.push_str("\\n"),
            '\r' => out.push_str("\\r"),
            '\t' => out.push_str("\\t"),
            c if (c as u32) < 0x20 => out.push_str(&format!("\\u{:04x}", c as u32)),
            c => out.push(c),
        }
    }
    out.push('"');
}

fn push_number(out: &mut String, value: f64) {
    if value.is_finite() {
        out.push_str(&value.to_string());
    } else {
        out.push_str("null");
    }
}

/// Parsea LRC enriquecido (marcas de línea y de palabra) a un array JSON.
#[wasm_bindgen]
pub fn parse_lrc(lrc: &str) -> String {
    let mut lines: Vec<Line> = Vec::new();

    for raw in lrc.split('\n') {
        let raw = raw.strip_suffix('\r').unwrap_or(raw);
        let (stamps, content) = line_stamps(raw);
        if stamps.is_empty() {
            continue;
        }
        let (raw_text, words) = word_times(&content);
        let text = raw_text.trim().to_string();
        if text.is_empty() {
            continue;
        }
        for time in stamps {
            lines.push(Line {
                time,
                end: 0.0,
                text: text.clone(),
                words: words
                    .iter()
                    .map(|word| Word {
                        time: time + word.time,
                        text: word.text.clone(),
                    })
                    .collect(),
            });
        }
    }

    // sort_by es estable, igual que Array.prototype.sort en JS.
    lines.sort_by(|a, b| {
        a.time
            .partial_cmp(&b.time)
            .unwrap_or(std::cmp::Ordering::Equal)
    });
    interpolate(&mut lines);

    let mut out = String::from("[");
    for (index, line) in lines.iter().enumerate() {
        if index > 0 {
            out.push(',');
        }
        out.push_str("{\"time\":");
        push_number(&mut out, line.time);
        out.push_str(",\"end\":");
        push_number(&mut out, line.end);
        out.push_str(",\"text\":");
        push_json_string(&mut out, &line.text);
        out.push_str(",\"words\":[");
        for (word_index, word) in line.words.iter().enumerate() {
            if word_index > 0 {
                out.push(',');
            }
            out.push_str("{\"time\":");
            push_number(&mut out, word.time);
            out.push_str(",\"text\":");
            push_json_string(&mut out, &word.text);
            out.push('}');
        }
        out.push_str("]}");
    }
    out.push(']');
    out
}

// ---------------------------------------------------------------------------
// Paleta: réplica del comportamiento de src/shared/utils/color.ts
// ---------------------------------------------------------------------------

struct Bucket {
    key: u32,
    count: u32,
    r: f64,
    g: f64,
    b: f64,
}

fn rgb_to_hsl(r: f64, g: f64, b: f64) -> (f64, f64, f64) {
    let red = r / 255.0;
    let green = g / 255.0;
    let blue = b / 255.0;
    let max = red.max(green).max(blue);
    let min = red.min(green).min(blue);
    let delta = max - min;

    let mut hue = 0.0;
    if delta != 0.0 {
        if max == red {
            hue = ((green - blue) / delta) % 6.0;
        } else if max == green {
            hue = (blue - red) / delta + 2.0;
        } else {
            hue = (red - green) / delta + 4.0;
        }
        hue *= 60.0;
        if hue < 0.0 {
            hue += 360.0;
        }
    }

    let lightness = (max + min) / 2.0;
    let saturation = if delta == 0.0 {
        0.0
    } else {
        delta / (1.0 - (2.0 * lightness - 1.0).abs())
    };
    (hue, saturation, lightness)
}

fn to_css(r: f64, g: f64, b: f64) -> String {
    let (hue, sat, light) = rgb_to_hsl(r, g, b);
    let boosted_sat = (sat * 100.0 + 18.0).clamp(45.0, 88.0).round() as i64;
    let boosted_light = (light * 100.0 + 8.0).clamp(46.0, 68.0).round() as i64;
    format!(
        "hsl({} {}% {}%)",
        hue.round() as i64, boosted_sat, boosted_light
    )
}

fn default_palette() -> String {
    "{\"primary\":\"hsl(252 70% 62%)\",\"secondary\":\"hsl(200 70% 55%)\",\"palette\":[\"hsl(252 70% 62%)\",\"hsl(200 70% 55%)\",\"hsl(300 60% 58%)\"]}".to_string()
}

/// Extrae colores dominantes de un buffer RGBA (bucket de 4 bits por canal).
#[wasm_bindgen]
pub fn palette(rgba: &[u8]) -> String {
    // Vec + búsqueda lineal para conservar el orden de inserción (como Map en JS).
    let mut buckets: Vec<Bucket> = Vec::new();
    let mut i = 0usize;
    while i + 3 < rgba.len() {
        let r8 = rgba[i];
        let g8 = rgba[i + 1];
        let b8 = rgba[i + 2];
        let a8 = rgba[i + 3];
        i += 4;
        if a8 < 128 {
            continue;
        }

        let r = r8 as f64;
        let g = g8 as f64;
        let b = b8 as f64;
        let (_, sat, light) = rgb_to_hsl(r, g, b);
        if light < 0.08 || light > 0.94 {
            continue;
        }
        if sat < 0.12 {
            continue;
        }

        let key = (((r8 as u32) >> 4) << 8) | (((g8 as u32) >> 4) << 4) | ((b8 as u32) >> 4);
        if let Some(bucket) = buckets.iter_mut().find(|bucket| bucket.key == key) {
            bucket.count += 1;
            bucket.r += r;
            bucket.g += g;
            bucket.b += b;
        } else {
            buckets.push(Bucket {
                key,
                count: 1,
                r,
                g,
                b,
            });
        }
    }

    let mut ranked: Vec<(u32, f64, f64, f64)> = buckets
        .iter()
        .map(|bucket| {
            (
                bucket.count,
                bucket.r / bucket.count as f64,
                bucket.g / bucket.count as f64,
                bucket.b / bucket.count as f64,
            )
        })
        .collect();
    ranked.sort_by(|a, b| b.0.cmp(&a.0));

    if ranked.is_empty() {
        return default_palette();
    }

    let first = ranked[0];
    let (h1, _, _) = rgb_to_hsl(first.1, first.2, first.3);

    let mut distinct = ranked[std::cmp::min(1, ranked.len() - 1)];
    for &candidate in &ranked {
        let (h2, _, _) = rgb_to_hsl(candidate.1, candidate.2, candidate.3);
        let diff = (h1 - h2).abs();
        if diff.min(360.0 - diff) > 40.0 {
            distinct = candidate;
            break;
        }
    }

    let mut colors: Vec<String> = ranked
        .iter()
        .take(3)
        .map(|c| to_css(c.1, c.2, c.3))
        .collect();
    if colors.len() < 2 {
        colors = vec![
            to_css(first.1, first.2, first.3),
            to_css(distinct.1, distinct.2, distinct.3),
            to_css(first.1, first.2, first.3),
        ];
    }

    let mut out = String::from("{\"primary\":");
    push_json_string(&mut out, &to_css(first.1, first.2, first.3));
    out.push_str(",\"secondary\":");
    push_json_string(&mut out, &to_css(distinct.1, distinct.2, distinct.3));
    out.push_str(",\"palette\":[");
    for (index, color) in colors.iter().enumerate() {
        if index > 0 {
            out.push(',');
        }
        push_json_string(&mut out, color);
    }
    out.push_str("]}");
    out
}
