(: usefull functions :)

module namespace fb = 'https://sites.google.com/site/fb2pdfj';

declare function fb:cut-right($string as xs:string?, $length as xs:integer) 
{ 
    replace(replace($string,concat('^(.{', $length, '}).+$'),'$1…'), '^(.*)\\W.*…', '$1…') 
}; 

declare function fb:cut-left($string as xs:string?, $length as xs:integer) 
{ 
    replace(replace($string,concat('^.+(.{', $length, '})$'),'…$1'), '…\\w*\\W(.*)$', '…$1') 
}; 
