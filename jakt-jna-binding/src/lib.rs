extern crate libc;
extern crate core;

use std::ffi::{CStr, CString};
use std::path::Path;
use serde_derive::Serialize;
use jakt::{JaktError, lexer, parser};
use jakt::lexer::Token;
use jakt::parser::ParsedFile;
use libc::c_char;
use serde::Serialize;

#[derive(Serialize)]
struct LexResult {
    tokens: Vec<Token>,
    error: Option<JaktError>,
}

#[derive(Serialize)]
enum ParseResult {
    Error(JaktError),
    File(ParsedFile),
}

fn lex_impl(bytes: &[u8]) -> LexResult {
    let (tokens, error) = lexer::lex(0, bytes);
    LexResult { tokens, error }
}

fn parse_impl(bytes: &[u8]) -> ParseResult {
    let result = lex_impl(bytes);
    if result.error.is_some() {
        return ParseResult::Error(result.error.unwrap())
    }

    let (file, error) = parser::parse_file(result.tokens.as_slice());

    if error.is_some() {
        return ParseResult::Error(error.unwrap());
    }

    ParseResult::File(file)
}

fn serialize<T: Serialize>(string: *const c_char, action: fn(&[u8]) -> T) -> *mut c_char {
    let string = unsafe {
        assert!(!string.is_null());
        CStr::from_ptr(string)
    };

    let parsed_result = action(string.to_bytes());
    let serialized_result = serde_json::to_string(&parsed_result).unwrap();
    CString::new(serialized_result).unwrap().into_raw()
}

#[no_mangle]
pub extern fn lex(string: *const c_char) -> *mut c_char {
    serialize(string, lex_impl)
}

#[no_mangle]
pub extern fn parse(string: *const c_char) -> *mut c_char {
    serialize(string, parse_impl)
}
