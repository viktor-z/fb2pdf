(: 
    There is handy GUI tool for editing and running XQuery that comes with BaseX
    Download from http://basex.org/products/download/all-downloads/
    Run bin\basexgui.bat for GUI

    This is an BaseX equivalent of the following fb2pdf transformation in stylesheet.json:
	{   // remove square brackets from note link
  		"query":   "//a[@type='note']/text()",
			"morpher": "replace(., '[\\[\\]]', '')"
	},
    
    Same script can also be run from command-line:
    > basex.bat debug.xquery > result.fb2
:)
declare default element namespace "http://www.gribuser.ru/xml/fictionbook/2.0"; 
declare namespace l = "http://www.w3.org/1999/xlink";

copy $doc := doc("c:/tmp/fb2pdf.aaa/_Test.fb2")
modify ( 
  let $query := $doc//a[@type='note']/text()            (: 'query' from fb2pdf goes here :)
  for $node in $query     
  return (
    let $morpher := replace($node, '[\[\]]', '')        (: 'morpher from fb2pdf goes here :)
    return replace node $node with $morpher
  )
)
return $doc
