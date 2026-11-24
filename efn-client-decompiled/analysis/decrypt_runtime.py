import sys
src, dst = sys.argv[1], sys.argv[2]
Kb = [90, 60, 113, 14, 45, 68, 25, 103]
d = bytearray(open(src, 'rb').read())
s = 324508639
for i in range(len(d)):
    s = (s * 1103515245 + 12345) & 0xFFFFFFFF
    d[i] ^= ((s >> 16) & 0xFF) ^ Kb[i & 7]
open(dst, 'wb').write(d)
print(len(d), d[:4].hex())
