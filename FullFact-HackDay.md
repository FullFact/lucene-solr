## summary of changes

* We [imported](solr/example/exampledocs/fullfact/facthack) the sample data and using a [utility script](solr/example/exampledocs/fullfact/wrap_xml.py) converted them into example input [files](solr/example/exampledocs) (and removed the existing example inputs).


* We added a synonyms [file](solr/server/solr/configsets/sample_techproducts_configs/conf/index_synonyms.txt) to be replaced in future by ...

* The `content_t` field is of `text_general` field type. We configured `solr.SynonymFilterFactory` using `index_synonyms.txt` for `text_general` field type in [managed-schema](solr/server/solr/configsets/sample_techproducts_configs/conf/managed-schema#L451)

* We increased the default `hl.maxAnalyzedChars` for the `/select` handler in [solrconfig.xml](solr/server/solr/configsets/sample_techproducts_configs/conf/solrconfig.xml#L780).

## how to see 'techproducts with fullfact example data'

git clone https://github.com/FullFact/lucene-solr
git checkout master-fullfact-hackday

cd solr
ant dist
and server

bin/solr start -e techproducts

http://localhost:8983/solr/techproducts/select?wt=json&fl=id&hl.fl=content_t&hl=on&indent=on&q=content_t:"_nn_ is rising"

http://localhost:8983/solr/#/techproducts/analysis?analysis.fieldvalue="NHS is rising"&analysis.fieldtype=text_general&verbose_output=1

bin/solr stop
