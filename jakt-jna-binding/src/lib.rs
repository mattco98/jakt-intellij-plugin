extern crate core;
extern crate libc;

use std::ffi::{CStr, CString};
use std::path::Path;

use jakt::{Compiler, JaktError, Project, Span};
use json;
use libc::c_char;

fn span_to_json(span: Span) -> json::JsonValue {
    json::object! {
        file_id: span.file_id,
        start: span.start,
        end: span.end,
    }
}

fn jakt_error_to_json(error: JaktError) -> json::JsonValue {
    let mut object = json::JsonValue::new_object();

    match error {
        JaktError::IOError(err) => {
            object["type"] = "IOError".into();
            object["message"] = err.to_string().into();
        }
        JaktError::StringError(err) => {
            object["type"] = "StringError".into();
            object["message"] = err.into();
        }
        JaktError::ParserError(err, span) => {
            object["type"] = "ParserError".into();
            object["message"] = err.into();
            object["span"] = span_to_json(span);
        }
        JaktError::ParserErrorWithHint(err_message, err_span, hint_msg, hint_span) => {
            object["type"] = "ParserErrorWithHint".into();
            object["message"] = err_message.into();
            object["span"] = span_to_json(err_span);
            object["hint_message"] = hint_msg.into();
            object["hint_span"] = span_to_json(hint_span);
        }
        JaktError::ValidationError(err, span) => {
            object["type"] = "ValidationError".into();
            object["message"] = err.into();
            object["span"] = span_to_json(span);
        }
        JaktError::TypecheckError(err, span) => {
            object["type"] = "TypecheckError".into();
            object["message"] = err.into();
            object["span"] = span_to_json(span);
        }
        JaktError::TypecheckErrorWithHint(err_message, err_span, hint_msg, hint_span) => {
            object["type"] = "TypecheckErrorWithHint".into();
            object["message"] = err_message.into();
            object["span"] = span_to_json(err_span);
            object["hint_message"] = hint_msg.into();
            object["hint_span"] = span_to_json(hint_span);
        }
    }

    object
}

fn typecheck_result(path: &Path) -> json::JsonValue {
    let mut project = Project::new();
    let mut compiler = Compiler::new(vec![]);

    match compiler.check_project(path, &mut project) {
        (_, Some(err)) => json::object! {
            "type": "Error",
            "error": jakt_error_to_json(err)
        },
        _ => json::object! { "type": "Ok" }
    }
}

#[no_mangle]
pub fn typecheck(path: *const c_char) -> *mut c_char {
    let path_string = unsafe {
        assert!(!path.is_null());
        CStr::from_ptr(path)
    };

    let path = Path::new(path_string.to_str().unwrap());
    let result = typecheck_result(path);
    CString::new(json::stringify(result)).unwrap().into_raw()
}
