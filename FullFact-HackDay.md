## summary of changes

* We [imported](solr/example/exampledocs/fullfact/facthack) the sample data and using a [utility script](solr/example/exampledocs/fullfact/wrap_xml.py) converted them into example input [files](solr/example/exampledocs) (and removed the existing example inputs).


* We added a synonyms [file](solr/server/solr/configsets/sample_techproducts_configs/conf/index_synonyms.txt) which mocks what a real part of speech analyser would produce, for a small number of sample words. A real POS tokenizer is still required.

* The `content_t` field is of `text_general` field type. We configured `solr.SynonymFilterFactory` using `index_synonyms.txt` for `text_general` field type in [managed-schema](solr/server/solr/configsets/sample_techproducts_configs/conf/managed-schema#L451)

* We increased the default `hl.maxAnalyzedChars` for the `/select` handler in [solrconfig.xml](solr/server/solr/configsets/sample_techproducts_configs/conf/solrconfig.xml#L780).

* It seesm you can not query easily for two tokens at the same position - for "rising" as a "verb". It is not possibly to match two tokens at the same position. This may limit the use.

## how to see 'techproducts with fullfact example data'

```
git clone https://github.com/FullFact/lucene-solr
git checkout master-fullfact-hackday

cd solr
ant dist
ant server

bin/solr start -e techproducts

http://localhost:8983/solr/techproducts/select?wt=json&fl=id&hl.fl=content_t&hl=on&indent=on&q=content_t:"_nn_ is rising"

http://localhost:8983/solr/#/techproducts/analysis?analysis.fieldvalue="NHS is rising"&analysis.fieldtype=text_general&verbose_output=1

bin/solr stop
```
