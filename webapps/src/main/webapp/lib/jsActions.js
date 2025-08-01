function initJSTreeActions()
{
    //treeActions.push(new GenomeBrowserAction());
    //treeActions.push(new OpenSequenceAction());
}

/*
 * Open genome browser action
 */
function GenomeBrowserAction () {
    var _this = this;
    this.id = "open_genome_browser",
    this.label = "Open track",
    this.icon = "icons/open.gif",
    
    this.isVisible = function(completePath)
    {
        if(!completePath) return -1;
        var dc = getDataCollection(completePath);
        if(!instanceOf(dc.getClass(), 'ru.biosoft.bsa.Track')) return -1;
        if(instanceOf(dc.getClass(), 'ru.biosoft.bsa.transformer.FastaSequenceCollection')) return -1;//We don't want to view fasta as a track
        return true;
    };
}

GenomeBrowserAction.prototype = new Action();
GenomeBrowserAction.prototype.doAction = function(path)
{
    if(!path) return;
            var dc = getDataCollection(path);
            if(!instanceOf(dc.getClass(), 'ru.biosoft.bsa.Track')) return;

            function withSequenceCollection(fun) {
                if(instanceOf(dc.getClass(), 'ru.biosoft.bsa.track.combined.CombinedTrack'))
                {
                    dc.getBeanFields('genomeSelector/sequenceCollectionPath', function(beanDPS)
                    {
                        var genomeSelector = beanDPS.getValue('genomeSelector');
                        sequenceCollection = genomeSelector.getValue('sequenceCollectionPath');
                        if(sequenceCollection)
                        {
                            fun(sequenceCollection);
                        }
                        else
                        {
                             createBeanEditorDialog('Configure genome for track', path, function(data) {
                                dc.invalidateCollection();
                                dc.getHtml(function(info)
                                {
                                    $('#info_area').html(info);
                                });
                                dc.getBeanFields('genomeSelector/sequenceCollectionPath', function(beanDPS)
                                {
                                    var genomeSelector = beanDPS.getValue('genomeSelector');
                                    sequenceCollection = genomeSelector.getValue('sequenceCollectionPath');
                                    if(sequenceCollection)
                                        fun(sequenceCollection);
                                })
                              }
                             , true);
                        }
                    })
                }
                else
                {
                    var sequenceCollection = dc.getAttributes()['SequencesCollection'];
                    if(sequenceCollection)
                    {
                        fun(sequenceCollection);
                    }
                    else
                    {
                         createBeanEditorDialog('Configure genome for track', path, function(data) {
                            dc.invalidateCollection();
                            dc.getHtml(function(info)
                            {
                                $('#info_area').html(info);
                            });
                            sequenceCollection = dc.getAttributes()['SequencesCollection'];
                            if(sequenceCollection)
                                fun(sequenceCollection);
                          }
                         , true);
                    }
                }
            }

            function openDocument(sequenceCollection) {
                var pos = dc.getAttributes()['defaultPosition'];
                var doc = opennedDocuments[allocateDocumentId('sequence_'+sequenceCollection)];
                if(doc)
                {
                    openDocumentTab(doc);
                    doc.addTrack(path);
                } else
                {
                    paramHash = {de: sequenceCollection, pos: pos};
                    CreateSequenceDocument(sequenceCollection, function (sequenceDoc) {
                        sequenceDoc.initialTracks = [path];
                        openDocumentTab(sequenceDoc);
                    });
                }
            }

            if( instanceOf(dc.getClass(), 'ru.biosoft.bsa.analysis.FilteredTrack') )
            {
                queryBean(path, {}, function(data){
                        var beanDPS = convertJSONToDPS(data.values);
                        if( beanDPS && beanDPS.properties )
                        {
                            var sourcePath = beanDPS.properties.sourcePath.getValue();
                            if(sourcePath)
                                dc = getDataCollection(sourcePath);
                        }
                        withSequenceCollection(openDocument);
                    } , function() {
                        withSequenceCollection(openDocument);
                    });
            }
            else
            {
                withSequenceCollection(openDocument);
            }
}

function OpenSequenceAction () {
    this.id = "open_sequence";
    this.label ="Open sequence";
    this.icon = "icons/open.gif";
    this.multi = false;
    this.useOriginalPath = false;
    this.isVisible = function(completePath)
    {
        var path = getElementPath(completePath);
        var name = getElementName(completePath);
        if(!path || !name) return -1;
        var dc = getDataCollection(path);
        var type = dc.getChildClass(name);
        if (instanceOf(type,'ru.biosoft.bsa.AnnotatedSequence') || instanceOf(type,'ru.biosoft.bsa.project.Project')) 
        {
            return true;
        }
        if (instanceOf(type,'ru.biosoft.access.core.DataCollection'))
        {
            var info = getDataCollection(completePath).getElementInfoAt(0);
            if(info && instanceOf(info['class'],'ru.biosoft.bsa.AnnotatedSequence'))
            {
                return true;
            }
        }
        return -1;
    };
    
}
OpenSequenceAction.prototype = new Action();
OpenSequenceAction.prototype.doAction = function(path)
    {
        var _this  = this;
        if (instanceOf(getDataCollection(path).getClass(),'ru.biosoft.bsa.AnnotatedSequence'))
        {
            var doc = opennedDocuments[allocateDocumentId('sequence_'+getElementPath(path))];
            if(doc)
            {
                doc.setPosition(getElementName(path)+':');
            }
        }
        CreateSequenceDocument( path, function (sequenceDoc) {
            openDocumentTab(sequenceDoc);
        });
    }
