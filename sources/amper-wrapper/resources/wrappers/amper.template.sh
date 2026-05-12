#!/bin/sh

#
# Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
#

# Possible environment variables:
#   AMPER_DOWNLOAD_ROOT        Maven repository to download Amper dist from.
#                              default: https://packages.jetbrains.team/maven/p/amper/amper
#   AMPER_JRE_DOWNLOAD_ROOT    Url prefix to download Amper JRE from.
#                              default: https:/
#   AMPER_BOOTSTRAP_CACHE_DIR  Cache directory to store extracted JRE and Amper distribution
#   AMPER_JAVA_HOME            JRE to run Amper itself (optional, does not affect compilation)
#   AMPER_JAVA_OPTIONS         JVM options to pass to the JVM running Amper (does not affect the user's application)
#   AMPER_NO_WELCOME_BANNER    Disables the first-run welcome message if set to a non-empty value

set -e -u

# The version of the Amper distribution to provision and use
amper_version=@AMPER_VERSION@
# Establish chain of trust from here by specifying exact checksum of Amper distribution to be run
amper_sha256=@AMPER_DIST_TGZ_SHA256@

AMPER_DOWNLOAD_ROOT="${AMPER_DOWNLOAD_ROOT:-https://packages.jetbrains.team/maven/p/amper/amper}"

@include:common.template.sh@

# ********** Project-local version detection **********

# 1. Search upwards for an executable `amper` file and/or `project.yaml`
# Sets wrapper_script to the found wrapper path, or empty string if not found.
find_project_context() {
  wrapper_script=""
  this_script="$(realpath "$0")"
  project_dir=$(pwd)
  while [ "$project_dir" != "/" ] && [ -n "$project_dir" ]; do
    wrapper_candidate="$project_dir/amper"
    if [ "$this_script" = "$wrapper_candidate" ]; then
      # Found itself (local wrapper case), no need to update any version or search further.
      return 1
    fi

    if [ -f "$wrapper_candidate" ] && [ -x "$wrapper_candidate" ]; then
      # Found the wrapper — check that a project context exists alongside it
      if [ -f "$project_dir/project.yaml" ] || [ -f "$project_dir/module.yaml" ]; then
        wrapper_script="$wrapper_candidate"
        return 0
      else
        echo "WARNING: Found wrapper script '$wrapper_candidate', but no project.yaml or module.yaml near it. Skipping." >&2
        # Continue the search
      fi
    elif [ -f "$project_dir/project.yaml" ]; then
      # Found project.yaml but no executable wrapper alongside it
      echo "WARNING: Found a project.yaml in '$project_dir', but the wrapper script is missing; using $amper_version." >&2
      return 1
    fi

    project_dir=$(dirname "$project_dir")
  done
  # Do not check root '/' - it's an unlikely candidate for a project

  return 1
}

parse_project_context() {
  # Parse amper_version and amper_sha256 from "$wrapper_script" without executing it.
  parsed_amper_version=$(
    sed -n 's/^amper_version=\([A-Za-z0-9._+-]\{1,\}\)[[:space:]]*$/\1/p' "$wrapper_script" \
      | head -n 1
  )
  parsed_amper_sha256=$(
    sed -n 's/^amper_sha256=\([0-9a-fA-F]\{64\}\)[[:space:]]*$/\1/p' "$wrapper_script" \
      | head -n 1
  )

  if [ -z "$parsed_amper_version" ]; then
    echo "ERROR: Suspicious local wrapper script: failed to detect the distribution version in '$wrapper_script'" >&2
    return 1
  fi
  if [ -z "$parsed_amper_sha256" ]; then
    echo "ERROR: Suspicious local wrapper script: failed to detect the distribution checksum in '$wrapper_script'" >&2
    return 1
  fi

  # overwrite builtin values and proceed
  amper_version=$parsed_amper_version
  amper_sha256=$parsed_amper_sha256
  return 0
}

if [ -z "${AMPER_WRAPPER_ALWAYS_USE_INTRINSIC_VERSION:-}" ]; then
  find_project_context && parse_project_context
fi

# ********** System detection **********

kernelName=$(uname -s)
case "$kernelName" in
  Darwin* )
    default_amper_cache_dir="$HOME/Library/Caches/JetBrains/Amper"
    ;;
  Linux* )
    default_amper_cache_dir="$HOME/.cache/JetBrains/Amper"
    ;;
  CYGWIN* | MSYS* | MINGW* )
    if command -v cygpath >/dev/null 2>&1; then
      default_amper_cache_dir=$(cygpath -u "$LOCALAPPDATA\JetBrains\Amper")
    else
      die "The 'cypath' command is not available, but Amper needs it. Use amper.bat instead, or try a Cygwin or MSYS environment."
    fi
    ;;
  *)
    die "Unsupported platform $kernelName"
    ;;
esac

amper_cache_dir="${AMPER_BOOTSTRAP_CACHE_DIR:-$default_amper_cache_dir}"

# ********** Provision Amper distribution **********

amper_url="$AMPER_DOWNLOAD_ROOT/org/jetbrains/amper/amper-cli/$amper_version/amper-cli-$amper_version-dist.tgz"
amper_target_dir="$amper_cache_dir/amper-cli-$amper_version"
download_and_extract "Amper distribution v$amper_version" "$amper_url" "$amper_sha256" 256 "$amper_cache_dir" "$amper_target_dir" "true"

# ********** Launch Amper **********

launcher_script="$amper_target_dir/bin/launcher.sh"

AMPER_WRAPPER_PATH="$(realpath "$0")" \
exec /bin/sh "$launcher_script" "$@"
