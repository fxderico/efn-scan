import re, sys

TOK = r"(?:'(?:\\u[0-9a-fA-F]{4}|\\.|[^'\\])'|\d+)"
CH  = r"'(?:\\u[0-9a-fA-F]{4}|\\.|[^'\\])'"
STR = r'"(?:[^"\\]|\\.)*"'

def lit(tok):
    tok = tok.strip()
    if tok.startswith("'"):
        b = tok[1:-1]
        if b.startswith('\\u'): return int(b[2:], 16)
        m = {'\\t':9,'\\n':10,'\\r':13,"\\'":39,'\\\\':92,'\\"':34,'\\b':8,'\\f':12,'\\0':0}
        if b in m: return m[b]
        return ord(b)
    return int(tok)

def chr_lit(v):
    c = chr(v)
    return {"'":"\\'", '\\':'\\\\', '\n':'\\n', '\r':'\\r', '\t':'\\t'}.get(c, c if 32 <= v < 127 else "\\u%04x" % v)

def s_esc(c):
    return {'"':'\\"', '\\':'\\\\', '\n':'\\n', '\r':'\\r', '\t':'\\t'}.get(c, c if (32 <= ord(c) < 127) else ("\\u%04x" % ord(c)))

def unq_char(t): return chr(lit(t))
def unq_str(t):
    body = t[1:-1]; out = []; i = 0
    while i < len(body):
        c = body[i]
        if c == '\\':
            n = body[i+1]
            if n == 'u': out.append(chr(int(body[i+2:i+6], 16))); i += 6; continue
            out.append({'n':'\n','t':'\t','r':'\r','"':'"','\\':'\\',"'":"'"}.get(n, n)); i += 2
        else: out.append(c); i += 1
    return ''.join(out)
def q(s): return '"' + ''.join(s_esc(c) for c in s) + '"'

src = open(sys.argv[1], encoding='utf-8').read()

# 1. fold (char)(a op b)
CAST = re.compile(r"\(char\)\(\s*(%s)\s*([-+^])\s*(%s)\s*\)" % (TOK, TOK))
def fold(m):
    a, op, b = lit(m.group(1)), m.group(2), lit(m.group(3))
    v = a + b if op == '+' else a - b if op == '-' else a ^ b
    return "'" + chr_lit(v & 0xFFFF) + "'"
src = CAST.sub(fold, src)

# 2. sequential state machine
arr_env, sb_env = {}, {}
PAT = re.compile("|".join([
  r"(?P<arrdecl>char\[\]\s+(?P<an>var\d+|\w+)\s*=\s*new char\[\]\s*\{(?P<ael>[^}]*)\}\s*;)",
  r"(?P<newstr_arr>new String\(\s*(?P<ns>\w+)\s*\))",
  r"(?P<barearr>new char\[\]\s*\{(?P<bel>[^}]*)\})",
  r"(?P<sbdecl>StringBuilder\s+(?P<sn>\w+)\s*=\s*new StringBuilder\(\)\s*;)",
  r"(?P<app>(?<![\w.])(?P<an2>\w+)\.append\(\s*(?P<av>%s|%s)\s*\)\s*;)" % (CH, STR),
  r"(?P<gapp>(?<![\w.])(?P<gn>\w+)\.append\()",
  r"(?P<tostr>(?<![\w.])(?P<tn>\w+)\.toString\(\))",
]))
def els(s): return ''.join(unq_char(t) for t in re.findall(CH, s))
out, pos = [], 0
for m in PAT.finditer(src):
    out.append(src[pos:m.start()]); pos = m.end()
    if m.group('arrdecl'):
        arr_env[m.group('an')] = els(m.group('ael'))
        out.append("/* %s = %s */" % (m.group('an'), q(arr_env[m.group('an')])))
    elif m.group('newstr_arr'):
        n = m.group('ns')
        out.append(q(arr_env[n]) if n in arr_env else m.group(0))
    elif m.group('barearr'):
        out.append(q(els(m.group('bel'))))
    elif m.group('sbdecl'):
        sb_env[m.group('sn')] = [True, []]   # pure, pieces
        out.append(m.group(0))
    elif m.group('app'):
        n = m.group('an2')
        if n in sb_env:
            v = m.group('av')
            sb_env[n][1].append(unq_char(v) if v.startswith("'") else unq_str(v))
            out.append("/* +%s */" % q(sb_env[n][1][-1]))
        else: out.append(m.group(0))
    elif m.group('gapp'):
        if m.group('gn') in sb_env: sb_env[m.group('gn')][0] = False
        out.append(m.group(0))
    elif m.group('tostr'):
        n = m.group('tn')
        out.append(q(''.join(sb_env[n][1])) if (n in sb_env and sb_env[n][0]) else m.group(0))
src = ''.join(out) + src[pos:]
# 3. fold "a" + "b"
for _ in range(5):
    src = re.sub(r'(%s)\s*\+\s*(%s)' % (STR, STR), lambda m: q(unq_str(m.group(1)) + unq_str(m.group(2))), src)
sys.stdout.write(src)
