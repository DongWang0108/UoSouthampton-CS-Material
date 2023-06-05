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

def exec_ner( file_chapter = None, ontonotes_file = None ) :

	# CHANGE CODE BELOW TO TRAIN A NER MODEL AND/OR USE REGEX GENERATE A SET OF BOOK CHARACTERS AND FILTERED SET OF NE TAGS (task 4)

	# Input >> www.gutenberg.org sourced plain text file for a chapter of a book
	# Output >> characters.txt = plain text set of extracted character names. one line per character name.

	# hardcoded output to show exactly what is expected to be serialized (you should change this)
	# only the allowed types for task 4 PERSON will be serialized
	max_files = 3000
	max_iter = 150
	train_sents = create_dataset(ontonotes_file, max_files=max_files)
 
	# create feature vectors for every sent
	X_train = [sent2features(s, word2features_func = task_word2features) for s in train_sents]
	Y_train = [sent2labels(s) for s in train_sents]

	test_sents = get_sents(file_chapter)
	X_test = [sent2features(s, word2features_func = task_word2features) for s in test_sents]
 
	# get the label set
	set_labels = set([])
	
	for n_sent in range(len(Y_train)) :
		for str_label in Y_train[n_sent] :
			set_labels.add( str_label )
	labels = list( set_labels )

	# remove 'O' label as we are not usually interested in how well 'O' is predicted
	#labels = list( crf.classes_ )
	labels.remove('O')

	# Train CRF model
	crf = task_train_crf_model( X_train, Y_train, max_iter, labels )
 
	Y_pred = crf.predict( X_test )
	dictNE = {
		"PERSON": []
	}
	
	entity_name = ''
	flag = []
    # get_entity form Y_pred
	for sent, label in zip(X_test, Y_pred):
		for x, y in zip(sent, label):
			if y[0] == 'B':
				if entity_name != '':
					m = dict((a,flag.count(a)) for a in flag)
					n = [k for k,v in m.items() if max(m.values())==v]
					dictNE[n[0]].append(entity_name.strip().lower())
					flag.clear()
					entity_name = ''
				entity_name += x['word'] + ' '
				flag.append(y[2:])
			elif y[0] == 'I':
				entity_name += x['word'] + ' '
				flag.append(y[2:])
			else:
				if entity_name != '':
					m = dict((a,flag.count(a)) for a in flag)
					n = [k for k,v in m.items() if max(m.values())==v]
					dictNE[n[0]].append(entity_name.strip().lower())
					flag.clear()
				flag.clear()
				entity_name = ''
			
		if entity_name != '':
			m = dict((a,flag.count(a)) for a in flag)
			n = [k for k,v in m.items() if max(m.values())==v]
			dictNE[n[0]].append(entity_name.strip().lower())
	
	# regex
	person = get_characters(file_chapter)
	
	for s in dictNE['PERSON']:
		s = re.findall(r'^\W*(.*?)\W*$', s)[0]
		s = re.sub(r'^\W+', '', s)
		if re.sub(r'\W+$', '', s):
			person.append(re.sub(r'\W+$', '', s))

	dictNE['PERSON'] = list(set(person))
	# DO NOT CHANGE THE BELOW CODE WHICH WILL SERIALIZE THE ANSWERS FOR THE AUTOMATED TEST HARNESS TO LOAD AND MARK

	# write out all PERSON entries for character list for subtask 4
	writeHandle = codecs.open( 'characters.txt', 'w', 'utf-8', errors = 'replace' )
	if 'PERSON' in dictNE :
		for strNE in dictNE['PERSON'] :
			writeHandle.write( strNE.strip().lower()+ '\n' )
	writeHandle.close()

def create_dataset(dataset_file, max_files=None) :
	# load parsed ontonotes dataset
	readHandle = codecs.open( dataset_file, 'r', 'utf-8', errors = 'replace' )
	str_json = readHandle.read()
	readHandle.close()
	dict_ontonotes = json.loads( str_json )

	# make a training and test split
	list_files = list( dict_ontonotes.keys() )
	if len(list_files) > max_files :
		list_files = list_files[ :max_files ]

	# sent = (tokens, pos, IOB_label)
	list_train = []
	for str_file in list_files :
		for str_sent_index in dict_ontonotes[str_file] :
			# ignore sents with non-PENN POS tags
			if 'XX' in dict_ontonotes[str_file][str_sent_index]['pos'] :
				continue
			if 'VERB' in dict_ontonotes[str_file][str_sent_index]['pos'] :
				continue

			list_entry = []

			# compute IOB tags for named entities (if any)
			ne_type_last = None
			need = False
			for nTokenIndex in range(len(dict_ontonotes[str_file][str_sent_index]['tokens'])) :
				strToken = dict_ontonotes[str_file][str_sent_index]['tokens'][nTokenIndex]
				strPOS = dict_ontonotes[str_file][str_sent_index]['pos'][nTokenIndex]
				ne_type = None
				if 'ne' in dict_ontonotes[str_file][str_sent_index] :
					dict_ne = dict_ontonotes[str_file][str_sent_index]['ne']
					if not 'parse_error' in dict_ne :
						for str_NEIndex in dict_ne :
							if nTokenIndex in dict_ne[str_NEIndex]['tokens'] :
								ne_type = dict_ne[str_NEIndex]['type']
								break
				# if ne_type != None :
				if ne_type in ['PERSON'] :
					need = True
					if ne_type == ne_type_last :
						strIOB = 'I-' + ne_type
					else :
						strIOB = 'B-' + ne_type
				else :
					strIOB = 'O'
				ne_type_last = ne_type

				list_entry.append( ( strToken, strPOS, strIOB ) )

			if need:
				list_train.append( list_entry )

	return list_train

def sent2features(sent, word2features_func = None):
	return [word2features_func(sent, i) for i in range(len(sent))]

def sent2labels(sent):
	return [label for token, postag, label in sent]

def sent2tokens(sent):
	return [token for token, postag, label in sent]

def task_word2features(sent, i):

	word = sent[i][0]
	postag = sent[i][1]

	features = {
		'word' : word,
		'postag': postag,

		# token shape
		'word.lower()': word.lower(),
		'word.isupper()': word.isupper(),
		'word.istitle()': word.istitle(),
		'word.isdigit()': word.isdigit(),

		# token suffix
		'word.suffix': word.lower()[-3:],

		# POS prefix
		'postag[:2]': postag[:2],
	}
	if i > 0:
		word_prev = sent[i-1][0]
		postag_prev = sent[i-1][1]
		features.update({
			'-1:word.lower()': word_prev.lower(),
			'-1:postag': postag_prev,
			'-1:word.lower()': word_prev.lower(),
			'-1:word.isupper()': word_prev.isupper(),
			'-1:word.istitle()': word_prev.istitle(),
			'-1:word.isdigit()': word_prev.isdigit(),
			'-1:word.suffix': word_prev.lower()[-3:],
			'-1:postag[:2]': postag_prev[:2],
		})
	else:
		features['BOS'] = True

	if i > 1:
		word2 = sent[i - 2][0]
		word2_postag = sent[i - 2][1]
		word1 = sent[i - 1][0]
		words = word2 + word1 + word
		features.update({
            '-2:word': word2,
            '-2:words': words,
            '-2:word.isdigit()': word2.isdigit(),
	    	'-2:word.lower()': word2.lower(),
			'-2:word.isupper()': word2.isupper(),
			'-2:word.istitle()': word2.istitle(),
			'-2:word.suffix': word2.lower()[-3:],
			'-2:postag[:2]': word2_postag[:2],
        })

	if i < len(sent)-1:
		word_next = sent[i+1][0]
		postag_next = sent[i+1][1]
		features.update({
			'+1:word.lower()': word_next.lower(),
			'+1:postag': postag_next,
			'+1:word.lower()': word_next.lower(),
			'+1:word.isupper()': word_next.isupper(),
			'+1:word.istitle()': word_next.istitle(),
			'+1:word.isdigit()': word_next.isdigit(),
			'+1:word.suffix': word_next.lower()[-3:],
			'+1:postag[:2]': postag_next[:2],
		})
	else:
		features['EOS'] = True

	return features

def task_train_crf_model( X_train, Y_train, max_iter, labels ) :
	# train CRF model using all possible transitions
	crf = sklearn_crfsuite.CRF(
		algorithm='lbfgs',
		c1=0.1,
		c2=0.1,
		max_iterations=max_iter,
		all_possible_transitions=True,
	)
	crf.fit(X_train, Y_train)
	return crf

def sentence_token_nltk(str):
    sent_tokenize_list = nltk.sent_tokenize(str)
    return sent_tokenize_list

def get_sents(chapterpath):
    with codecs.open(chapterpath, 'r', encoding='utf-8') as f:
        str = f.read()

    str = re.sub(r'(\r\n){2,100}', 'hduwheduwhdiwdhwudwuduiwbsbshbhbbddbu', str)
    str = re.sub(r'\r\n', ' ', str)
    str = re.sub('hduwheduwhdiwdhwudwuduiwbsbshbhbbddbu', ' ', str)

    sents = sentence_token_nltk(str)

    sents = [nltk.pos_tag(nltk.word_tokenize(sent)) for sent in sents]

    return sents

def get_characters(chapter_path):
	with codecs.open(chapter_path, 'r', encoding='utf-8') as f:
		str = f.read()
	str = re.sub(r'(\r\n){2,100}', 'hduwheduwhdiwdhwudwuduiwbsbshbhbbddbu', str)
	str = re.sub(r'\r\n', '', str)
	str = re.sub('hduwheduwhdiwdhwudwuduiwbsbshbhbbddbu', r'\r\n', str)

	partern = r'(Mr\.\s|Mrs\.\s|Miss\s|[A-Z]\.\s)([A-Z]\w+)[ ,\.\;()’]'
	person = re.findall(partern, str, flags=re.MULTILINE)
	person1 = [(x+y).lower() for x, y in person]
	person2 = [y.lower() for x, y in person]
	person = person1 + person2

	return person

# if __name__ == '__main__':
#     chapter_file = './eval_chapter.txt'
#     ontonotes_file = './ontonotes_parsed/ontonotes_parsed.json'
#
#     # DO NOT CHANGE THE CODE IN THIS FUNCTION
#
#     exec_ner(chapter_file, ontonotes_file)
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

	exec_ner( chapter_file, ontonotes_file )
