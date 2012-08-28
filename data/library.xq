(: usefull functions :)

module namespace fb = 'https://sites.google.com/site/fb2pdfj';

declare default element namespace "http://www.gribuser.ru/xml/fictionbook/2.0"; 
declare namespace l = "http://www.w3.org/1999/xlink";

declare namespace file-utils   = "org.apache.commons.io.FileUtils";
declare namespace base64   = "org.apache.commons.codec.binary.Base64";
declare namespace file   = "java.io.File";

declare function fb:cut-right($string as xs:string?, $length as xs:integer) 
{ 
    replace(replace($string,concat('^(.{', $length, '}).+$'),'$1…'), '^(.*)\W.*…', '$1…') 
}; 

declare function fb:cut-left($string as xs:string?, $length as xs:integer) 
{ 
    replace(replace($string,concat('^.+(.{', $length, '})$'),'…$1'), '…\w*\W(.*)$', '…$1') 
}; 

declare function fb:note-sections($root as node()?) as node()* {
    let $hrefs := $root//a[@type ='note']/substring(@l:href, 2)
    return $root//body[@name]//section[@id = $hrefs]
};

declare function fb:load-binary($path as xs:string?) 
{ 
    base64:encodeBase64String(file-utils:readFileToByteArray(file:new($path))) 
}; 

