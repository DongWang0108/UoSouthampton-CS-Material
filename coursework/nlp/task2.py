# !/usr/bin/env python
# -*- coding: utf-8 -*-

######################################################################
#
# (c) Copyright University of Southampton, 2021
#
# Copyright in this software belongs to University of Southampton,
# Highfield, University Road, Southampton SO17 1BJ
#
# Created By : Stuart E. Middleton
# Created Date : 2021/01/29
# Project : Teaching
#
######################################################################

from __future__ import absolute_import, division, print_function, unicode_literals

import sys, codecs, json, math, time, warnings, re, logging
warnings.simplefilter( action='ignore', category=FutureWarning )

import nltk, numpy, scipy, sklearn, sklearn_crfsuite, sklearn_crfsuite.metrics

LOG_FORMAT = ('%(levelname) -s %(asctime)s %(message)s')
logger = logging.getLogger( __name__ )
logging.basicConfig( level=logging.INFO, format=LOG_FORMAT )
logger.info('logging started')

def exec_regex_questions( file_chapter = None ) :

	# CHANGE BELOW CODE TO USE REGEX TO LIST ALL QUESTIONS IN THE CHAPTER OF TEXT (task 2)

	# Input >> www.gutenberg.org sourced plain text file for a chapter of a book
	# Output >> questions.txt = plain text set of extracted questions. one line per question.

	# hardcoded output to show exactly what is expected to be serialized

    with codecs.open(file_chapter, 'r', 'utf-8') as f:
        strtext = f.read()

    pattern = r'(?<!\S)([a-zA-Z0-9\s\"\,\<\>\(\)\{\}\[\]\/\-\'\_\—\‘’\è]*\?)'


    information = re.findall(pattern, strtext, flags=re.MULTILINE)

    list3 = []

    for i in information:
        i0 = re.sub(r'^[^\w]+', '', i)
        i1 = re.sub(r'\n', '', i0)
        i2 = re.sub(r'\r', ' ', i1)
        list3.append(i2)
    print(list3)
    replace_list = ['‘', '"', '\'', '“', "’"]
    for j in range(len(list3)):
        sentence = list3[j]
        num = len(sentence)
        for char in range(num):
            if str.isalpha(sentence[char]):
                # print(sentence[char])
                continue
            else:
                for rp in replace_list:
                    if sentence[char] == rp:
                        if str.isalpha(sentence[char + 1]) and str.isalpha(sentence[char + 2]):
                            list3[j] = list3[j][char + 1:]
                            break

    setQuestions = set()
    for word in list3:
        setQuestions.add(word)
    writeHandle = codecs.open( 'questions.txt', 'w', 'utf-8', errors = 'replace' )
    for strQuestion in setQuestions :
        writeHandle.write( strQuestion + '\n' )
    writeHandle.close()

if __name__ == '__main__':
	if len(sys.argv) < 4 :
		raise Exception( 'missing command line args : ' + repr(sys.argv) )
	ontonotes_file = sys.argv[1]
	book_file = sys.argv[2]
	chapter_file = sys.argv[3]

	logger.info( 'ontonotes = ' + repr(ontonotes_file) )
	logger.info( 'book = ' + repr(book_file) )
	logger.info( 'chapter = ' + repr(chapter_file) )

	# DO NOT CHANGE THE CODE IN THIS FUNCTION

	exec_regex_questions( chapter_file )