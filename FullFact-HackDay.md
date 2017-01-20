## Problem

A "Stats" query to find "*something* is rising".
Where "something" is a noun phrase or similar.

The proposed solution was to include part of speech mark up at the same position during tokenisation, similar to synonyms.
Then you can query for something like
```
_nn_ is rising
```

Entity and other mark up could be included in the same way.
A consistent token policy for each annotation is required. We just prefixed and post fixed with "_".


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

## Model each claim as a single Solr Document
An additional consideration is related coreNLP.
Stanford coreNLP is the NLP library currently used for text pre-processing in FullFact.
It could be interesting to model one Solr Document per potential claim.
For example a claim could be modeled with the following fields :
* Root Verb
* Subject
* Object
* Author
            
Content of the fields could be plain text or entities ( if we put NER in the game).
Plain text and entities can live in separate fields to be consistent.
This is not a simple solution as the structure(s) of a claim must be defined from a grammar perspective 
(to be able to parse the dependency graph and map it to the claim document).
It may require a lot of simplification and the definition of supported grammar structures.
So it is not just navigating the edges of the dependency graph returned by coreNLP and push them to Solr.

This code snippets will show how to use coreNLP through Java API to explore the annotations that can be used in Solr.
            
N.B. this is a very basic code snippet and to work nicely we need to put much more effort in identifying the root verbs
and related subjects and objects.
```
HttpSolrClient.Builder builder = new HttpSolrClient.Builder("http://localhost:8983/solr/facts");
        HttpSolrClient solrClient = builder.build();

        String text = prepareText();

        Properties annotationProperties = new Properties();
        annotationProperties.setProperty("annotators", "tokenize,ssplit,pos,depparse");
        StanfordCoreNLPClient stanfordProcessingPipeline = new StanfordCoreNLPClient(annotationProperties, "http://corenlp.run", 80, 2);
        Annotation document = new Annotation(text);

        stanfordProcessingPipeline.annotate(document);

        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            String sentenceText = sentence.get(CoreAnnotations.TextAnnotation.class);
            Integer id = sentence.get(CoreAnnotations.TokenBeginAnnotation.class);

            SemanticGraph sentenceDependencies = sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);

            Collection<IndexedWord> roots = sentenceDependencies.getRoots();
            
            for (IndexedWord claimRoot : roots) {
                SolrInputDocument exampleDoc = new SolrInputDocument();
                exampleDoc.addField("id", claimRoot.beginPosition() + "-" + id);
                exampleDoc.addField("text", sentenceText);
                exampleDoc.addField("dependencies", sentenceDependencies);

                List<SemanticGraphEdge> outcomingEdges = sentenceDependencies.getOutEdgesSorted(claimRoot);
                StringBuilder verbBuilder = new StringBuilder();
                verbBuilder.append(claimRoot.value());
                for (SemanticGraphEdge edge : outcomingEdges) {
                    if (edge.getRelation().getShortName().equals("aux")) {
                        verbBuilder.insert(0, " " + edge.getTarget().value() + " ");
                    }
                    if (edge.getRelation().getShortName().equals("nsubj")) {
                        exampleDoc.addField("nsubj", edge.getTarget().value());
                    }
                }
                exampleDoc.addField("root", verbBuilder.toString());

                try {
                    solrClient.add(exampleDoc);
                    solrClient.commit();
                } catch (SolrServerException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
```
