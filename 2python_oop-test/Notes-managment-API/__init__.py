##package-level imports

#Package initialization — lets you from notes_api import app instead of from notes_api.main import app

#Expose public API — define __all__ or re-export symbols at the package level:
#from .api import router

#Run package-level setup — database connections, middleware, logging, etc.

#Shared state/config — store variables accessible to all modules in the package