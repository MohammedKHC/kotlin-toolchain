#!/bin/sh

#
# Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
#

# Kotlin CLI Installer
# Possible environment variables:
#   KOTLIN_CLI_NO_MODIFY_PATH    If set to a non-empty value, skip modifying the user PATH.
#   KOTLIN_CLI_DOWNLOAD_ROOT     If set, use this URL as the base for downloading Kotlin CLI artifacts.

set -e -u

kotlin_cli_version="{{KOTLIN_TOOLCHAIN_VERSION}}"
kotlin_cli_wrapper_sha256="{{KOTLIN_CLI_WRAPPER_SHA256}}"

repo_base_url="${KOTLIN_CLI_DOWNLOAD_ROOT:-"https://packages.jetbrains.team/maven/p/amper/amper"}/org/jetbrains/kotlin"
wrapper_name="kotlin"
install_dir="$HOME/.local/bin" # NOTE: This is also hardcoded in messages, change those as well if changed

# ********** Utility functions **********

{{include:die.template.sh}}
{{include:check_sha.template.sh}}

# ********** Download wrapper **********

# Downloads a file from the given URL to the given target path.
download_to_file() {
  url="$1"
  target="$2"

  temp_file="${target}.tmp.$$"
  rm -f "$temp_file"

  if command -v curl >/dev/null 2>&1; then
    curl --silent --show-error -L --fail --retry 5 --connect-timeout 30 --output "$temp_file" "$url"
  elif command -v wget >/dev/null 2>&1; then
    wget -nv --tries=5 --connect-timeout=30 --read-timeout=120 -O "$temp_file" "$url"
  else
    die "ERROR: Please install 'curl' or 'wget', as the Kotlin CLI installer needs one of them."
  fi

  mv "$temp_file" "$target"
}

# ********** Detect existing installation **********

# Warns if the wrapper is found on PATH in a directory other than install_dir.
warn_shadowed_installations() {
  if ! command -v "$wrapper_name" >/dev/null 2>&1; then
    return
  fi

  # Resolve install_dir to an absolute path for comparison
  resolved_install_dir=$(cd "$install_dir" 2>/dev/null && pwd) || resolved_install_dir="$install_dir"

  # Check all locations on PATH (not just the first one)
  IFS=:
  for path_dir in $PATH; do
    if [ -x "$path_dir/$wrapper_name" ]; then
      resolved_path_dir=$(cd "$path_dir" 2>/dev/null && pwd) || resolved_path_dir="$path_dir"
      if [ "$resolved_path_dir" != "$resolved_install_dir" ]; then
        echo "WARNING: Another '$wrapper_name' found at $path_dir/$wrapper_name" >&2
        echo "         It may shadow or be shadowed by the script in $install_dir" >&2
      fi
    fi
  done
  unset IFS
}

# ********** PATH management **********

# Checks if the given directory is already in PATH.
is_in_path() {
  dir_to_check="$1"
  case ":${PATH}:" in
    *":${dir_to_check}:"*) return 0 ;;
  esac
  return 1
}

# Adds the install directory to PATH via shell profile files.
add_install_dir_to_path() {
  target_dir="$install_dir"

  if [ -n "${KOTLIN_CLI_NO_MODIFY_PATH:-}" ]; then
    return 1
  fi

  export_line='export PATH="$HOME/.local/bin:$PATH"'

  shell_name=$(basename "${SHELL:-}" 2>/dev/null || echo "")

  # Determine which profile files to try based on the user's shell
  case "$shell_name" in
    zsh)
      # .zshrc is preferred; .zprofile is the fallback (also sourced by login shells)
      profile_files="$HOME/.zshrc:$HOME/.zprofile"
      ;;
    bash)
      kernelName=$(uname -s)
      # macOS terminals open login shells (.bash_profile); Linux terminals open non-login shells (.bashrc)
      case "$kernelName" in
        Darwin*)
          profile_files="$HOME/.bash_profile:$HOME/.profile"
          ;;
        *)
          profile_files="$HOME/.bashrc:$HOME/.profile"
          ;;
      esac
      ;;
    fish)
      profile_files="$HOME/.config/fish/config.fish"
      export_line="fish_add_path \"$target_dir\""
      ;;
    *)
      profile_files="$HOME/.profile"
      ;;
  esac

  # Check if the dir is already referenced in any profile file
  IFS=:
  for file in $profile_files; do
    if [ -f "$file" ] && grep -q "$target_dir" "$file" 2>/dev/null; then
      return 0
    fi
  done

  # Try to add the export line to the first writable profile file
  for file in $profile_files; do
    profile_dir=$(dirname "$file")
    if [ ! -d "$profile_dir" ]; then
      mkdir -p "$profile_dir" 2>/dev/null || continue
    fi
    if { printf '\n%s\n' "$export_line" >> "$file"; } 2>/dev/null; then
      echo "Added \$HOME/.local/bin to PATH in $file"
      return 0
    fi
  done
  unset IFS

  return 1
}

# ********** Main **********

main() {
  echo "Installing Kotlin CLI..."

  wrapper_url="${repo_base_url}/kotlin-cli/${kotlin_cli_version}/kotlin-cli-${kotlin_cli_version}-wrapper"

  if [ -f "$install_dir/$wrapper_name" ]; then
    echo "Existing installation found in $install_dir, it will be overwritten."
  fi

  warn_shadowed_installations

  mkdir -p "$install_dir"

  echo "Downloading Kotlin CLI wrapper script..."
  target_path="$install_dir/$wrapper_name"
  download_to_file "$wrapper_url" "$target_path"
  check_sha "$wrapper_url" "$target_path" "$kotlin_cli_wrapper_sha256" 256

  chmod +x "$target_path"
  KOTLIN_CLI_WRAPPER_ALWAYS_USE_INTRINSIC_VERSION="1" \
  "$target_path" --version

  echo "Installed Kotlin CLI to $target_path"
  echo ''
  if is_in_path "$install_dir"; then
    echo 'To get started you can run, e.g., `kotlin --help`'
  else
    if add_install_dir_to_path; then
      # This can't work immediately, so advise
      echo 'To get started you can either:'
      echo '  - restart your shell for it to pick up the PATH changes'
      echo '  - run `export PATH="$HOME/.local/bin:$PATH"`'
    else
      echo 'To get started you should:'
      echo ' 1. Manually add `$HOME/.local/bin` to your PATH'
      echo ' 2. Restart your shell'
    fi
    echo 'and then run, e.g., `kotlin --help`'
  fi
}

main
