#!/bin/bash
# i)  Automatically download pdb file from pdb website within a list
# ii) Automatically analyze which pdb file contains DNA and extract DNA coordinates
#     into files with chain name.
# iii)Summary report is generated at designated file
#  
# --*--Author: Zhenghong Dong (Michael)         --*--
# --*--Hunter College Non-degree student 		--*--
# --*--Date: 2-22-2014                          --*--
# --*--Version: 1.0 							--*--
# --*--Tested at Ubuntu 13.10LTS desktop version--*--
# --*--Bash version 4.2.45						--*--
# --*--Tested at CentOS6.5 						--*--
# --*--Bash version 4.1.2(1)					--*--

#set -x

#Initilize default value for global variants.
INPUT_FILE=""
OUTPUT_FILE="summary.txt"
DOWNLOAD_DIR="output"
OPTS_AND_ARGS=($@)

#Define error code.
E_WRONG_INPUT=50
E_NETWORK_CONNECTION=60

#Prompt usage information when illegal inputs detected.
usage()
{
	echo "Usage: $0 -i input_file -o output_file"

}

#getURL function translates PDB file with name 'XXXX' into downloadable urls.
getURL()
{
	_FILE_PREFIX="pdb"
	_FILE_SURFIX=".ent.gz"

	_BASE_URL="ftp://ftp.wwpdb.org/pub/pdb/data/structures/divided/pdb/"
	
	if [ $# -ne 1 ];then
		echo "Wrong number of argments was entered in getURL function"
		exit $E_WRONG_INPUT
	fi
	
	_PDB_NAME=$1

	if [ ${#_PDB_NAME} != 4 ];then
		echo "Invalid PDB name was entered"
		exit $E_WRONG_INPUT
	fi
	
	_PDB_NAME_LOWERCASE=`echo $_PDB_NAME|tr '[A-Z]' '[a-z]'`
	_FILE_MIDDLE=`echo ${_PDB_NAME_LOWERCASE:1:2}`
	_URL=$_BASE_URL$_FILE_MIDDLE"/"$_FILE_PREFIX$_PDB_NAME_LOWERCASE$_FILE_SURFIX
	echo $_URL

}

#downloadWithListFile reads pdb name list from the INPUT_FILE and download, unzip the 
#pdb files in .gz form.
downloadWithListFile()
{
	_INPUT_FILE=$1

	#Create output folder
	if [ ! -d $DOWNLOAD_DIR ]; then
		mkdir $DOWNLOAD_DIR
	fi	
	
	#Test network connection
	_GOOGLE_URL_FOR_TEST="http://www.google.com"
	curl --connect-timeout 7 --max-time 15 --head --silent "$_GOOGLE_URL_FOR_TEST"|grep 'HTTP/1.1 200 OK'
	if [ $? -ne 0 ]; then
		echo "Unable to connect to internet, please check network connection."
		exit $E_NETWORK_CONNECTION
	fi
	
	#Download pdb files and unzip them
	while read LINE
	do
		COMPLETE_URL=`getURL $LINE`
		curl -o $DOWNLOAD_DIR/$LINE.ent.gz  $COMPLETE_URL
		gunzip -f $DOWNLOAD_DIR/$LINE.ent.gz 
	
	done<$_INPUT_FILE
}

#analyzePDB search pdb file for DNA lines, split the found lines into different
#chains and write into corresponding named files, generate summary reports
analyzePDB()
{

	declare -a _PDB_WITHOUT_DNA
	declare -a _PDB_WITH_DNA

	cd $DOWNLOAD_DIR
	declare -a _FILE_LIST=(`ls|grep 'ent$'`)

	#counters that counts the numbers of pdb file with or without DNA
	#for the usage of for loop
	_POSITIVE_COUNTER=0
	_NEGATIVE_COUNTER=0
	
	if [ ${#_FILE_LIST[*]} -eq 0 ];then
		echo "No file has been downloaded or error happened while unzipping."
		exit 15
	fi
	
	for _PDB_FILE in ${_FILE_LIST[*]}
	do
		grep ATOM $_PDB_FILE | grep -E '\<D[ATGC]\>' 2>&1 > /dev/null
		case $?	in
		0)  
			_PDB_WITH_DNA[$_POSITIVE_COUNTER]=$_PDB_FILE
			_POSITIVE_COUNTER=`expr $_POSITIVE_COUNTER + 1`

			grep -n -E '\<D[ATGC]\>' $_PDB_FILE|grep ATOM|sort -k5n|awk '{print >$5"_Chain_"fileName}' fileName="${_PDB_FILE:0:4}"
			;;
		*)
			_PDB_WITHOUT_DNA[$_NEGATIVE_COUNTER]=$_PDB_FILE
			_NEGATIVE_COUNTER=`expr $_NEGATIVE_COUNTER + 1`
			;;
		esac
	done
	
	# Make summary report.	
	echo "Total number of files is "`expr $_POSITIVE_COUNTER + $_NEGATIVE_COUNTER`>$OUTPUT_FILE
	echo "The number of file(s) with DNA is "$_POSITIVE_COUNTER>>$OUTPUT_FILE
	echo "Files with DNA include ${_PDB_WITH_DNA[*]}">>$OUTPUT_FILE
	
	echo "The number of file(s) without DNA is "$_NEGATIVE_COUNTER>>$OUTPUT_FILE
	echo "Files without DNA include ${_PDB_WITHOUT_DNA[*]}">>$OUTPUT_FILE
}

#parse the options and arguments of this entire script
parseOpts()
{
	#Test if the arguments are valid
	_ARGS=($@)
	if [ ${#_ARGS[@]} -ne 4 ];then
		usage
		exit $E_WRONG_INPUT
	fi
	
	while getopts ":i:o:" arg
	do 
		case $arg in
			i) if [ ! -e $OPTARG ];then
					echo "Input_file not designated or not exist. "
					usage
					exit $E_WRONG_INPUT
					
				else
					INPUT_FILE=$OPTARG
					
				fi
				;;

			o) if [ -z "$OPTARG" ];then

					echo "No output file designated, summary file will use default: ./"$DOWNLOAD_DIR'/summary.txt'
					echo "DNA chains of each file will be in the folder: ./"$DOWNLOAD_DIR
				else
					#Replace the default OUTPUT_FILE with user designated output
					OUTPUT_FILE=$OPTARG
					echo "Summary will be logged into: ./"$DOWNLOAD_DIR"/"$OUTPUT_FILE
					echo "DNA chains of each file will be in the folder: ./"$DOWNLOAD_DIR
				fi
				;;

			*) usage
				exit $E_WRONG_INPUT
				;;
		esac
	done
}




# Script runs from here.

parseOpts ${OPTS_AND_ARGS[*]}

downloadWithListFile $INPUT_FILE

analyzePDB $OUTPUT_FILE


