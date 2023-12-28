#!/usr/bin/env bash
#
#    Copyright 2009-2023 the original author or authors.
#
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#    You may obtain a copy of the License at
#
#       https://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
#


base_path=$(cd $(dirname $BASH_SOURCE) && pwd)

fonts_path=$(cd ${base_path}/.. && pwd)/fonts

case "$(uname -s)" in

  Darwin)
    # Do something under Mac OS X platform
    # https://apple.stackexchange.com/a/240382
    target_path=$HOME/Library/Fonts
    ;;

  Linux)
    # Do something under GNU/Linux platform
    target_path=$HOME/.fonts
    ;;

  CYGWIN*|MINGW32*|MSYS*|MINGW*)
    echo 'MS Windows'
    ;;

  *)
    echo 'Other OS'
    ;;
esac

if [ -z "${target_path}" ]
then
  echo "\$target_path is empty."
  exit 1;
fi

if [ ! -d "${target_path}" ]; then
  echo "mkdir ${target_path}"
  mkdir -p $target_path
fi

for ffile in `ls ${fonts_path}`;
do
  if [[ ! -f "${target_path}/${ffile}" ]]; then
    echo "install $ffile"
    cp "${fonts_path}/${ffile}" "${target_path}/${ffile}"
  fi
done

## Config font for BlockDiagram
blockdiagrc=${HOME}/.blockdiagrc

if [[ -f "${target_path}/SourceHanSerifSC-Regular.otf" ]]; then
  if [[ ! -f "${blockdiagrc}" ]]; then
    echo "create file: ${blockdiagrc}"
    touch ${blockdiagrc}
  fi
fi

if ! grep -q "seqdiag" "${blockdiagrc}"; then
  echo "config font for seqdiag."
  echo -e "\n[seqdiag]\nfontpath = ${target_path}/SourceHanSerifSC-Regular.otf" >> ${blockdiagrc}
fi

if ! grep -q "blockdiag" "${blockdiagrc}"; then
  echo "config font for blockdiag."
  echo -e "\n[blockdiag]\nfontpath = ${target_path}/SourceHanSerifSC-Regular.otf" >> ${blockdiagrc}
fi

if ! grep -q "actdiag" "${blockdiagrc}"; then
  echo "config font for actdiag."
  echo -e "\n[actdiag]\nfontpath = ${target_path}/SourceHanSerifSC-Regular.otf" >> ${blockdiagrc}
fi

if ! grep -q "nwdiag" "${blockdiagrc}"; then
  echo "config font for nwdiag."
  echo -e "\n[nwdiag]\nfontpath = ${target_path}/SourceHanSerifSC-Regular.otf" >> ${blockdiagrc}
fi