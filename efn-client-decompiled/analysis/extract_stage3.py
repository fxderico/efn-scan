#!/usr/bin/env python3
"""
Offline extractor for the EFN stage-3 container (/cdn/v2/9f4e7a2c1b8d.png), mirroring
RuntimeBridge.extractHidsChunk + zlibInflate + unzipIntoWorkDir:
  PNG -> first chunk of type 'hIds' -> zlib inflate -> ZIP (python.exe, AppHost/main.py, ...)
Lists the archive and optionally extracts it. Never executes anything.

  python3 -I extract_stage3.py <file.png> [--out DIR]
"""
import argparse, io, struct, sys, zipfile, zlib, os

def hids_payload(data: bytes) -> bytes:
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("not a PNG")
    off = 8
    while off + 8 <= len(data):
        length, ctype = struct.unpack(">I4s", data[off:off + 8])
        if ctype == b"hIds":
            return zlib.decompress(data[off + 8: off + 8 + length])
        off += 12 + length          # len + type + data + crc
    raise ValueError("no hIds chunk")

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("png"); ap.add_argument("--out")
    a = ap.parse_args()
    blob = hids_payload(open(a.png, "rb").read())
    print(f"hIds payload: {len(blob)} bytes inflated")
    z = zipfile.ZipFile(io.BytesIO(blob))
    for i in z.infolist():
        print(f"{i.file_size:>12}  {i.filename}")
    if a.out:
        root = os.path.realpath(a.out)
        for i in z.infolist():   # zip-slip safe (the malware itself is not)
            dst = os.path.realpath(os.path.join(root, i.filename))
            if not dst.startswith(root + os.sep):
                print("skipping unsafe path", i.filename, file=sys.stderr); continue
            z.extract(i, root)
        print("extracted to", root)

if __name__ == "__main__":
    main()
