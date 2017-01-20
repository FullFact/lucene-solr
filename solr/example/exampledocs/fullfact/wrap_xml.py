#!/usr/bin/env python

import sys

def main(fileNames):

  for inFileName in fileNames:
    with open(inFileName, 'r') as inFile:

      inFileNamePieces = inFileName.split('/')
      docId = inFileNamePieces[len(inFileNamePieces)-1]

      idFieldSnippet = '<field name="id">'+docId+'</field>'
      textFieldSnippetStart = '<field name="content_t">'
      textFieldSnippetEnd = '</field>'

      # TODO: replace the replace hack below
      outFileName = inFileName.replace('debates','wrapped-debates')
      with open(outFileName, 'w') as outFile:
        for line in inFile:
          line = line.replace('<add>','<add>\n<doc>\n'+idFieldSnippet+'\n'+textFieldSnippetStart)
          line = line.replace('</add>',textFieldSnippetEnd+'</doc>\n</add>')
          outFile.write(line)

if __name__ == '__main__':
  if (len(sys.argv) > 1):
    main(sys.argv[1:])
  else:
    print "Usage: "+sys.argv[0]+" <list of files to index>"

