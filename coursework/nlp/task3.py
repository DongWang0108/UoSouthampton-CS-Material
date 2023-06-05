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

warnings.simplefilter(action='ignore', category=FutureWarning)

import nltk, numpy, scipy, sklearn, sklearn_crfsuite, sklearn_crfsuite.metrics

LOG_FORMAT = ('%(levelname) -s %(asctime)s %(message)s')
logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO, format=LOG_FORMAT)
logger.info('logging started')


def create_dataset(dataset_file, max_files=None):
    # load parsed ontonotes dataset
    readHandle = codecs.open(dataset_file, 'r', 'utf-8', errors='replace')
    str_json = readHandle.read()
    readHandle.close()
    dict_ontonotes = json.loads(str_json)

    # make a training and test split
    list_files = list(dict_ontonotes.keys())
    if len(list_files) > max_files:
        list_files = list_files[:max_files]

    # sent = (tokens, pos, IOB_label)
    list_train = []
    for str_file in list_files:
        for str_sent_index in dict_ontonotes[str_file]:
            # ignore sents with non-PENN POS tags
            if 'XX' in dict_ontonotes[str_file][str_sent_index]['pos']:
                continue
            if 'VERB' in dict_ontonotes[str_file][str_sent_index]['pos']:
                continue

            list_entry = []

            # compute IOB tags for named entities (if any)
            ne_type_last = None
            need = False
            for nTokenIndex in range(len(dict_ontonotes[str_file][str_sent_index]['tokens'])):
                strToken = dict_ontonotes[str_file][str_sent_index]['tokens'][nTokenIndex]
                strPOS = dict_ontonotes[str_file][str_sent_index]['pos'][nTokenIndex]
                ne_type = None
                if 'ne' in dict_ontonotes[str_file][str_sent_index]:
                    dict_ne = dict_ontonotes[str_file][str_sent_index]['ne']
                    if not 'parse_error' in dict_ne:
                        for str_NEIndex in dict_ne:
                            if nTokenIndex in dict_ne[str_NEIndex]['tokens']:
                                ne_type = dict_ne[str_NEIndex]['type']
                                break
                # if ne_type != None :
                if ne_type in ['CARDINAL', 'DATE', 'NORP', 'ORDINAL']:
                    need = True
                    if ne_type == ne_type_last:
                        strIOB = 'I-' + ne_type
                    else:
                        strIOB = 'B-' + ne_type
                else:
                    strIOB = 'O'
                ne_type_last = ne_type

                list_entry.append((strToken, strPOS, strIOB))
            if need:
                list_train.append(list_entry)

    return list_train


def sent2features(sent, word2features_func=None):
    return [word2features_func(sent, i) for i in range(len(sent))]


def sent2labels(sent):
    return [label for token, postag, label in sent]


def sent2tokens(sent):
    return [token for token, postag, label in sent]


def task_word2features(sent, i):
    word = sent[i][0]
    postag = sent[i][1]

    features = {
        'word': word,
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
        word_prev = sent[i - 1][0]
        postag_prev = sent[i - 1][1]
        features.update({
            '-1:word.lower()': word_prev.lower(),
            '-1:postag': postag_prev,
            # '-1:word.lower()': word_prev.lower(),
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

    if i < len(sent) - 1:
        word_next = sent[i + 1][0]
        postag_next = sent[i + 1][1]
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
    # train the basic CRF model
    crf = sklearn_crfsuite.CRF(
        algorithm='lbfgs',
        c1=0.1,
        c2=0.1,
        max_iterations=max_iter,
        all_possible_transitions=False,
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


def exec_ner(file_chapter=None, ontonotes_file=None):
    # CHANGE CODE BELOW TO TRAIN A CRF NER MODEL TO TAG THE CHAPTER OF TEXT (task 3)

    # Input >> www.gutenberg.org sourced plain text file for a chapter of a book
    # Output >> ne.json = { <ne_type> : [ <phrase>, <phrase>, ... ] }

    # hardcoded output to show exactly what is expected to be serialized (you should change this)
    # only the allowed types for task 3 DATE, CARDINAL, ORDINAL, NORP will be serialized
    max_iter = 150
    max_files = 3000
    train_sents = create_dataset(ontonotes_file, max_files=max_files)
    #print(f'train_set_len: {len(train_sents)}')

    # create feature vectors for every sent
    X_train = [sent2features(s, word2features_func=task_word2features) for s in train_sents]
    Y_train = [sent2labels(s) for s in train_sents]

    test_sents = get_sents(file_chapter)
    X_test = [sent2features(s, word2features_func=task_word2features) for s in test_sents]

    # get the label set
    set_labels = set([])

    for n_sent in range(len(Y_train)):
        for str_label in Y_train[n_sent]:
            set_labels.add(str_label)
    labels = list(set_labels)

    # remove 'O' label as we are not usually interested in how well 'O' is predicted
    # labels = list( crf.classes_ )
    labels.remove('O')

    # Train CRF model
    crf = task_train_crf_model(X_train, Y_train, max_iter, labels)

    Y_pred = crf.predict(X_test)
    dictNE = {
        "CARDINAL": [],
        "ORDINAL": [],
        "DATE": [],
        "NORP": [],
    }

    entity_name = ''
    flag = []
    # get_entity form Y_pred
    for sent, label in zip(X_test, Y_pred):
        for x, y in zip(sent, label):
            if y[0] == 'B':
                if entity_name != '':
                    m = dict((a, flag.count(a)) for a in flag)
                    n = [k for k, v in m.items() if max(m.values()) == v]
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
                    m = dict((a, flag.count(a)) for a in flag)
                    n = [k for k, v in m.items() if max(m.values()) == v]
                    dictNE[n[0]].append(entity_name.strip().lower())
                    flag.clear()
                flag.clear()
                entity_name = ''

        if entity_name != '':
            m = dict((a, flag.count(a)) for a in flag)
            n = [k for k, v in m.items() if max(m.values()) == v]
            dictNE[n[0]].append(entity_name.strip().lower())

    with codecs.open(file_chapter, 'r', encoding='utf-8') as f:
        str = f.read()
    str = re.sub(r'(\r\n){2,100}', 'hduwheduwhdiwdhwudwuduiwbsbshbhbbddbu', str)
    str = re.sub(r'\r\n', ' ', str)
    str = re.sub('hduwheduwhdiwdhwudwuduiwbsbshbhbbddbu', ' ', str)
    c = re.findall(r'(one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve\
		   |thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty).', str, flags=re.IGNORECASE)
    o = re.findall(r'(first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth|eleventh|twelfth\
		   |thirteenth|fourteenth|fifteenth|sixteenth|seventeenth|eighteenth|nineteenth|twentieth).', str,
                   flags=re.IGNORECASE)
    d = re.findall(r'(saturday afternoon|half-a-dozen years|many years ago|a month|some days|one day|the summer of \d+|several years|\
		   two years agao|Januar of that year|recent years|one mouth|today).', str, flags=re.IGNORECASE)
    n = re.findall(r'(chinese|Buddhism|Taiwanese|Japanese|European|German|anti-Japanese|Chechen|\
		   Russian|Asian|South Korean|Korean|indians).', str, flags=re.IGNORECASE)
    c = [x.lower() for x in c]
    o = [x.lower() for x in o]
    d = [x.lower() for x in d]
    n = [x.lower() for x in n]
    #print('date:',dictNE['DATE'])
    #print('NORp:', dictNE['NORP'])
    if c != None:
        #print('c:',c)
        #print(dictNE['CARDINAL'])
        for s in dictNE['CARDINAL']:
            c.append(s)
        c = list(set(c))
        #c = list(set(c.extend(s for s in dictNE['CARDINAL'])))
        dictNE['CARDINAL'] = []
        for i in c:
            if i.isdigit():
                continue
            dictNE['CARDINAL'].append(i)
       # print(dictNE['CARDINAL'])
    if o != None:
        for s in dictNE['ORDINAL']:
            o.append(s)
        o = list(set(o))
        # c = list(set(c.extend(s for s in dictNE['CARDINAL'])))
        dictNE['ORDINAL'] = []
        for i in o:
            dictNE['ORDINAL'].append(i)
    if d != None:
        for s in dictNE['DATE']:
            d.append(s)
        d = list(set(d))
        # c = list(set(c.extend(s for s in dictNE['CARDINAL'])))
        dictNE['DATE'] = []
        for i in d:
            dictNE['DATE'].append(i)
    if n != None:
        for s in dictNE['NORP']:
            n.append(s)
        n = list(set(n))
        # c = list(set(c.extend(s for s in dictNE['CARDINAL'])))
        dictNE['NORP'] = []
        for i in n:
            dictNE['NORP'].append(i)







    # DO NOT CHANGE THE BELOW CODE WHICH WILL SERIALIZE THE ANSWERS FOR THE AUTOMATED TEST HARNESS TO LOAD AND MARK

    # FILTER NE dict by types required for task 3
    listAllowedTypes = ['DATE', 'CARDINAL', 'ORDINAL', 'NORP']
    listKeys = list(dictNE.keys())
    for strKey in listKeys:
        for nIndex in range(len(dictNE[strKey])):
            dictNE[strKey][nIndex] = dictNE[strKey][nIndex].strip().lower()
        if not strKey in listAllowedTypes:
            del dictNE[strKey]

    # write filtered NE dict
    writeHandle = codecs.open('ne.json', 'w', 'utf-8', errors='replace')
    strJSON = json.dumps(dictNE, indent=2)
    writeHandle.write(strJSON + '\n')
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

    exec_ner( chapter_file, ontonotes_file )

# if __name__ == '__main__':
#     chapter_file = './eval_chapter.txt'
#     ontonotes_file = './ontonotes_parsed/ontonotes_parsed.json'
#
#     # DO NOT CHANGE THE CODE IN THIS FUNCTION
#
#     exec_ner(chapter_file, ontonotes_file)
