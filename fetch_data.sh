#!/bin/bash

usage() 
{ 
  echo 'Fetch N data files (index 0 to [N-1]) from the server.'
  echo 'Usage: ./fetch_data.sh [N] [username] [IP] [target path]'
}

if [ $# -ne 4 ]; then
  usage
  exit 1
else
  N=$1
  USERNAME=$2
  IP=$3 
  TARGET=$4

  for i in $(seq 0 1 "$((${N}-1))")
  do
    echo Downloading file "$i" 
    scp "$USERNAME"@"$IP":/mnt/disk/Yahoo_data/new_sorted_data/fmat_input_files/"$i".fmat $TARGET
  done
fi

