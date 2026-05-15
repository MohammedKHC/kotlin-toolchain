# usage: check_sha SOURCE_MONIKER FILE SHA_CHECKSUM SHA_SIZE
# $1 SOURCE_MONIKER (e.g. url)
# $2 FILE
# $3 SHA hex string
# $4 SHA size in bits (256, 512, ...)
check_sha() {
  sha_size=$4
  if command -v shasum >/dev/null 2>&1; then
    echo "$3 *$2" | shasum -a "$sha_size" --status -c || {
      die "ERROR: Checksum mismatch for $2 (downloaded from $1): expected checksum $3 but got $(shasum --binary -a "$sha_size" "$2" | awk '{print $1}')"
    }
    return 0
  fi

  shaNsumCommand="sha${sha_size}sum"
  if command -v "$shaNsumCommand" >/dev/null 2>&1; then
    # discard the output as sha*sum may print redundant warnings in some versions
    echo "$3 *$2" | $shaNsumCommand -w -c >/dev/null 2>&1 || {
      die "ERROR: Checksum mismatch for $2 (downloaded from $1): expected checksum $3 but got $($shaNsumCommand "$2" | awk '{print $1}')"
    }
    return 0
  fi

  echo "Both 'shasum' and 'sha${sha_size}sum' utilities are missing. Please install one of them"
  return 1
}
