from pydantic import BaseModel, EmailStr
from typing import Optional
from datetime import datetime
from decimal import Decimal

class ExporterBase(BaseModel):
    name: str
    email: EmailStr
    phone: Optional[str] = None
    address: Optional[str] = None
    company_registration: Optional[str] = None
    tax_id: Optional[str] = None

class ExporterCreate(ExporterBase):
    pass

class Exporter(ExporterBase):
    id: int
    is_active: bool
    created_at: datetime
    deleted_at: Optional[datetime] = None
    
    class Config:
        from_attributes = True

class SupplierBase(BaseModel):
    supplier_type: str
    name: str
    contact_person: Optional[str] = None
    phone: Optional[str] = None
    email: Optional[str] = None

class SupplierCreate(SupplierBase):
    exporter_id: int
    gps_coordinates: Optional[dict] = None

class Supplier(SupplierBase):
    id: int
    exporter_id: int
    gps_coordinates: Optional[dict] = None
    verification_status: str
    created_at: datetime
    deleted_at: Optional[datetime] = None
    
    class Config:
        from_attributes = True

class FarmBase(BaseModel):
    farm_name: Optional[str] = None
    farm_code: Optional[str] = None
    elevation: Optional[str] = None
    variety: Optional[str] = None
    harvest_period: Optional[str] = None

class FarmCreate(FarmBase):
    supplier_id: int
    boundary_polygon: Optional[dict] = None

class Farm(FarmBase):
    id: int
    supplier_id: int
    boundary_polygon: Optional[dict] = None
    created_at: datetime
    deleted_at: Optional[datetime] = None
    
    class Config:
        from_attributes = True

class CoffeeLotBase(BaseModel):
    lot_id: str
    name: Optional[str] = None
    origin_region: Optional[str] = None
    weight_kg: Optional[Decimal] = None

class CoffeeLotCreate(CoffeeLotBase):
    exporter_id: int
    harvest_start: Optional[datetime] = None
    harvest_end: Optional[datetime] = None

class CoffeeLot(CoffeeLotBase):
    id: int
    exporter_id: int
    status: str
    created_at: datetime
    deleted_at: Optional[datetime] = None
    
    class Config:
        from_attributes = True

class ShipmentBase(BaseModel):
    shipment_code: str
    buyer_name: Optional[str] = None
    destination_port: Optional[str] = None

class ShipmentCreate(ShipmentBase):
    coffee_lot_id: int

class Shipment(ShipmentBase):
    id: int
    coffee_lot_id: int
    departure_date: Optional[datetime] = None
    arrival_date: Optional[datetime] = None
    status: str
    created_at: datetime
    deleted_at: Optional[datetime] = None
    
    class Config:
        from_attributes = True

class DocumentBase(BaseModel):
    document_type: str
    original_filename: Optional[str] = None

class DocumentCreate(DocumentBase):
    documentable_type: str
    documentable_id: int
    file_path: str
    uploaded_by: Optional[str] = None

class Document(DocumentBase):
    id: int
    documentable_type: str
    documentable_id: int
    file_path: str
    uploaded_by: Optional[str] = None
    created_at: datetime
    deleted_at: Optional[datetime] = None
    
    class Config:
        from_attributes = True

class BuyerBase(BaseModel):
    company_name: str
    contact_email: EmailStr

class BuyerCreate(BuyerBase):
    exporter_id: int

class Buyer(BuyerBase):
    id: int
    exporter_id: int
    access_token: str
    created_at: datetime
    deleted_at: Optional[datetime] = None
    
    class Config:
        from_attributes = True

class UserBase(BaseModel):
    email: EmailStr
    role: Optional[str] = "exporter_admin"

class UserCreate(UserBase):
    password: str
    exporter_id: int

class User(UserBase):
    id: int
    exporter_id: int
    created_at: datetime
    deleted_at: Optional[datetime] = None
    
    class Config:
        from_attributes = True

class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"